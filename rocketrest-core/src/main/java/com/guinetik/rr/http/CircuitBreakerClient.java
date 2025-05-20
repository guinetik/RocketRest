package com.guinetik.rr.http;

import com.guinetik.rr.request.RequestSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * A RocketClient implementation that adds circuit breaker functionality
 * to any underlying RocketClient implementation.
 * <p>
 * The circuit breaker pattern prevents cascading failures by failing fast when
 * a service appears to be unhealthy, allowing it time to recover without being
 * overwhelmed by requests.
 */
public class CircuitBreakerClient implements RocketClient {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerClient.class);

    /**
     * Circuit breaker state
     * @see HttpConstants.CircuitBreaker#STATUS_CLOSED
     * @see HttpConstants.CircuitBreaker#STATUS_OPEN
     * @see HttpConstants.CircuitBreaker#STATUS_HALF_OPEN
     */
    public enum State {
        /** Normal operation - {@link HttpConstants.CircuitBreaker#STATUS_CLOSED} */
        CLOSED,
        /** Circuit is open, fast-fail - {@link HttpConstants.CircuitBreaker#STATUS_OPEN} */
        OPEN,
        /** Testing if service is back - {@link HttpConstants.CircuitBreaker#STATUS_HALF_OPEN} */
        HALF_OPEN
    }

    /**
     * Strategy for differentiating between failures
     */
    public enum FailurePolicy {
        /** Counts all exceptions as failures */
        ALL_EXCEPTIONS,
        /** Only count status codes {@link HttpConstants.StatusCodes#SERVER_ERROR_MIN} (500) */
        SERVER_ERRORS_ONLY,
        /** Exclude status codes in range {@link HttpConstants.StatusCodes#CLIENT_ERROR_MIN} (400) to 
            {@link HttpConstants.StatusCodes#CLIENT_ERROR_MAX} (499) */
        EXCLUDE_CLIENT_ERRORS,
        /** Use custom predicate */
        CUSTOM
    }

    private final RocketClient delegate;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final long failureDecayTimeMs;
    private final FailurePolicy failurePolicy;
    private final Predicate<RocketRestException> failurePredicate;
    
    // Metrics
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicInteger rejectedRequests = new AtomicInteger(0);
    private final AtomicInteger circuitTrips = new AtomicInteger(0);
    private final Map<Integer, AtomicInteger> statusCodeCounts = new ConcurrentHashMap<>();

    /**
     * Creates a circuit breaker with default settings
     * 
     * @param delegate The underlying client implementation
     */
    public CircuitBreakerClient(RocketClient delegate) {
        this(delegate, 
             HttpConstants.CircuitBreaker.DEFAULT_FAILURE_THRESHOLD,
             HttpConstants.CircuitBreaker.DEFAULT_RESET_TIMEOUT_MS);
    }

    /**
     * Creates a circuit breaker with custom threshold and timeout
     * 
     * @param delegate The underlying client implementation
     * @param failureThreshold Number of failures before opening circuit
     * @param resetTimeoutMs Time in milliseconds before trying to close circuit
     */
    public CircuitBreakerClient(RocketClient delegate, int failureThreshold, long resetTimeoutMs) {
        this(delegate, failureThreshold, resetTimeoutMs, 
             HttpConstants.CircuitBreaker.DEFAULT_FAILURE_DECAY_TIME_MS,
             FailurePolicy.ALL_EXCEPTIONS, null);
    }

    /**
     * Creates a fully customized circuit breaker
     * 
     * @param delegate The underlying client implementation
     * @param failureThreshold Number of failures before opening circuit
     * @param resetTimeoutMs Time in milliseconds before trying to close circuit
     * @param failureDecayTimeMs Time after which failure count starts to decay
     * @param failurePolicy Strategy to determine what counts as a failure
     * @param failurePredicate Custom predicate if policy is CUSTOM
     */
    public CircuitBreakerClient(RocketClient delegate, int failureThreshold, long resetTimeoutMs,
                               long failureDecayTimeMs, FailurePolicy failurePolicy,
                               Predicate<RocketRestException> failurePredicate) {
        this.delegate = delegate;
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
        this.failureDecayTimeMs = failureDecayTimeMs;
        this.failurePolicy = failurePolicy;
        
        // Set default predicate based on policy if not provided
        if (failurePolicy == FailurePolicy.CUSTOM && failurePredicate != null) {
            this.failurePredicate = failurePredicate;
        } else {
            this.failurePredicate = createDefaultPredicate(failurePolicy);
        }
    }

    @Override
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
        // Check for periodic decay reset
        checkFailureDecay();
        
        // Track metrics
        totalRequests.incrementAndGet();
        
        // Check circuit state
        State currentState = state.get();
        if (currentState == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() >= resetTimeoutMs) {
                // Try moving to HALF_OPEN
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    logger.info(HttpConstants.CircuitBreaker.LOG_CIRCUIT_HALF_OPEN);
                    currentState = State.HALF_OPEN;
                }
            } else {
                // Track metrics
                rejectedRequests.incrementAndGet();
                
                // Get time since last failure
                long now = System.currentTimeMillis();
                long millisSinceFailure = now - lastFailureTime.get();
                long remainingMillis = resetTimeoutMs - millisSinceFailure;
                
                // We're in OPEN state and the timeout hasn't elapsed, so fast-fail with circuit breaker exception
                throw new CircuitBreakerOpenException(
                    HttpConstants.CircuitBreaker.CIRCUIT_OPEN,
                    millisSinceFailure,
                    resetTimeoutMs
                );
            }
        }

        try {
            // Execute the request with the delegate client
            Res response = delegate.execute(requestSpec);

            // Success - reset circuit if needed
            if (currentState != State.CLOSED) {
                state.set(State.CLOSED);
                failureCount.set(0);
                logger.info(HttpConstants.CircuitBreaker.LOG_CIRCUIT_CLOSED);
            }
            
            // Track metrics
            successfulRequests.incrementAndGet();
            
            return response;
        } catch (RocketRestException e) {
            // Track all failures in metrics
            failedRequests.incrementAndGet();
            
            // Track status code in metrics if available
            int statusCode = e.getStatusCode();
            if (statusCode > 0) {
                statusCodeCounts.computeIfAbsent(statusCode, code -> new AtomicInteger(0))
                                .incrementAndGet();
            }
            
            // Handle failure according to policy
            boolean isCountableFailure = shouldCountAsFailure(e);
            if (isCountableFailure) {
                handleFailure(e);
            }
            
            // If we just opened the circuit from this failure, wrap the exception
            // to indicate the circuit is now open
            if (isCountableFailure && currentState == State.CLOSED && state.get() == State.OPEN) {
                throw new CircuitBreakerOpenException(
                    "Circuit opened due to failure: " + e.getMessage(),
                    e,
                    0,  // Just opened, so 0 time since failure
                    resetTimeoutMs
                );
            }
            
            // Otherwise rethrow the original exception
            throw e;
        }
    }
    
    /**
     * Performs a health check by trying to execute the given request
     * This can be used to manually test if the service is healthy
     * 
     * @param <Req> Request type
     * @param <Res> Response type
     * @param healthCheckRequest The request to use as a health check
     * @return true if the service is healthy
     */
    public <Req, Res> boolean performHealthCheck(RequestSpec<Req, Res> healthCheckRequest) {
        try {
            delegate.execute(healthCheckRequest);
            
            // If we get here, service is healthy, close circuit
            if (state.get() != State.CLOSED) {
                state.set(State.CLOSED);
                failureCount.set(0);
                logger.info(HttpConstants.CircuitBreaker.LOG_CIRCUIT_CLOSED);
            }
            
            return true;
        } catch (RocketRestException e) {
            // Service still failing
            if (state.get() == State.HALF_OPEN) {
                state.set(State.OPEN);
                lastFailureTime.set(System.currentTimeMillis());
                logger.warn(HttpConstants.CircuitBreaker.LOG_TEST_FAILED);
            }
            
            return false;
        }
    }
    
    /**
     * Manually resets the circuit to closed state
     */
    public void resetCircuit() {
        state.set(State.CLOSED);
        failureCount.set(0);
        logger.info(HttpConstants.CircuitBreaker.LOG_CIRCUIT_CLOSED + " (manual reset)");
    }
    
    /**
     * Gets current circuit breaker state
     * 
     * @return Current state (OPEN, CLOSED, HALF_OPEN)
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * Gets current failure count
     * 
     * @return Current failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Gets circuit breaker metrics
     * 
     * @return Map of metric name to value
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Basic metrics
        metrics.put("state", getStateAsString());
        metrics.put("failureCount", failureCount.get());
        metrics.put("failureThreshold", failureThreshold);
        metrics.put("totalRequests", totalRequests.get());
        metrics.put("successfulRequests", successfulRequests.get());
        metrics.put("failedRequests", failedRequests.get());
        metrics.put("rejectedRequests", rejectedRequests.get());
        metrics.put("circuitTrips", circuitTrips.get());
        
        // Add status code counts
        Map<String, Integer> statusCounts = new HashMap<>();
        statusCodeCounts.forEach((code, count) -> statusCounts.put(code.toString(), count.get()));
        metrics.put("statusCodes", statusCounts);
        
        // Time metrics
        long lastFailure = lastFailureTime.get();
        if (lastFailure > 0) {
            metrics.put("millisSinceLastFailure", System.currentTimeMillis() - lastFailure);
        }
        
        return Collections.unmodifiableMap(metrics);
    }
    
    /**
     * Gets the state as a string constant
     * 
     * @return State as a string defined in HttpConstants
     */
    private String getStateAsString() {
        switch (state.get()) {
            case OPEN:
                return HttpConstants.CircuitBreaker.STATUS_OPEN;
            case CLOSED:
                return HttpConstants.CircuitBreaker.STATUS_CLOSED;
            case HALF_OPEN:
                return HttpConstants.CircuitBreaker.STATUS_HALF_OPEN;
            default:
                return state.get().toString();
        }
    }

    private void handleFailure(RocketRestException e) {
        if (state.get() == State.HALF_OPEN) {
            // Failed during test request
            state.set(State.OPEN);
            lastFailureTime.set(System.currentTimeMillis());
            logger.warn(HttpConstants.CircuitBreaker.LOG_TEST_FAILED);
            return;
        }

        int currentFailures = failureCount.incrementAndGet();
        if (currentFailures >= failureThreshold && state.compareAndSet(State.CLOSED, State.OPEN)) {
            lastFailureTime.set(System.currentTimeMillis());
            logger.warn(HttpConstants.CircuitBreaker.LOG_CIRCUIT_OPENED, currentFailures);
            circuitTrips.incrementAndGet();
        }
    }
    
    /**
     * Checks if it's time to decay the failure count
     */
    private void checkFailureDecay() {
        long now = System.currentTimeMillis();
        long lastReset = lastResetTime.get();
        
        // If we're in CLOSED state and decay time has passed, reset failure count
        if (state.get() == State.CLOSED && failureCount.get() > 0 && 
                (now - lastReset) >= failureDecayTimeMs) {
            if (failureCount.getAndSet(0) > 0) {
                logger.debug(HttpConstants.CircuitBreaker.LOG_DECAY_RESET);
            }
            lastResetTime.set(now);
        }
    }
    
    /**
     * Creates appropriate failure predicate based on policy
     */
    private Predicate<RocketRestException> createDefaultPredicate(FailurePolicy policy) {
        switch (policy) {
            case SERVER_ERRORS_ONLY:
                return e -> e.getStatusCode() >= HttpConstants.StatusCodes.SERVER_ERROR_MIN &&
                            e.getStatusCode() <= HttpConstants.StatusCodes.SERVER_ERROR_MAX;
            case EXCLUDE_CLIENT_ERRORS:
                return e -> e.getStatusCode() < HttpConstants.StatusCodes.CLIENT_ERROR_MIN || 
                            e.getStatusCode() > HttpConstants.StatusCodes.CLIENT_ERROR_MAX;
            case ALL_EXCEPTIONS:
            default:
                return e -> true;
        }
    }
    
    /**
     * Determines if an exception should count toward failure threshold based on policy
     */
    private boolean shouldCountAsFailure(RocketRestException e) {
        return failurePredicate.test(e);
    }

    @Override
    public void configureSsl(SSLContext sslContext) {
        delegate.configureSsl(sslContext);
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.delegate.setBaseUrl(baseUrl);
    }
}