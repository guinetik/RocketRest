package com.guinetik.rr.interceptor;

import com.guinetik.rr.http.CircuitBreakerOpenException;
import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.http.RocketRestException;
import com.guinetik.rr.request.RequestSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Interceptor that retries failed requests with configurable backoff.
 *
 * <p>This interceptor catches exceptions during request execution and retries
 * them based on configurable criteria. It supports exponential backoff,
 * maximum retry limits, and custom retry predicates.
 *
 * <h2>Default Retry Behavior</h2>
 * <p>By default, retries are attempted for:
 * <ul>
 *   <li>5xx Server Errors (500-599)</li>
 *   <li>408 Request Timeout</li>
 *   <li>429 Too Many Requests</li>
 *   <li>Network/Connection errors (status code 0 or -1)</li>
 * </ul>
 *
 * <p>The following are NOT retried:
 * <ul>
 *   <li>4xx Client Errors (except 408, 429)</li>
 *   <li>{@link CircuitBreakerOpenException} (circuit is open)</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre class="language-java"><code>
 * // Retry up to 3 times with 1 second initial delay
 * RetryInterceptor retry = new RetryInterceptor(3, 1000);
 *
 * RocketClient client = RocketClientFactory.builder("https://api.example.com")
 *     .withInterceptor(retry)
 *     .build();
 * </code></pre>
 *
 * <h2>Exponential Backoff</h2>
 * <pre class="language-java"><code>
 * // Retry with exponential backoff: 1s, 2s, 4s
 * RetryInterceptor retry = new RetryInterceptor(3, 1000, 2.0);
 * </code></pre>
 *
 * <h2>Custom Retry Predicate</h2>
 * <pre class="language-java"><code>
 * // Only retry on specific status codes
 * RetryInterceptor retry = RetryInterceptor.builder()
 *     .maxRetries(3)
 *     .initialDelayMs(500)
 *     .retryOn(e -&gt; e.getStatusCode() == 503)
 *     .build();
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RequestInterceptor
 * @see InterceptorChain
 * @since 1.1.0
 */
public class RetryInterceptor implements RequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RetryInterceptor.class);

    /** Default status codes that trigger retry */
    private static final Set<Integer> DEFAULT_RETRYABLE_STATUS_CODES = new HashSet<Integer>(
        Arrays.asList(
            HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR,  // 500
            HttpConstants.StatusCodes.BAD_GATEWAY,            // 502
            HttpConstants.StatusCodes.SERVICE_UNAVAILABLE,    // 503
            504,  // Gateway Timeout
            408,  // Request Timeout
            429   // Too Many Requests
        )
    );

    private final int maxRetries;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;
    private final Predicate<RocketRestException> retryPredicate;

    /**
     * Creates a retry interceptor with default settings.
     * Uses 3 retries, 1 second initial delay, 2x backoff, 30 second max delay.
     */
    public RetryInterceptor() {
        this(3, 1000, 2.0, 30000, null);
    }

    /**
     * Creates a retry interceptor with custom retry count and delay.
     *
     * @param maxRetries Maximum number of retries (must be positive)
     * @param initialDelayMs Initial delay between retries in milliseconds
     */
    public RetryInterceptor(int maxRetries, long initialDelayMs) {
        this(maxRetries, initialDelayMs, 2.0, 30000, null);
    }

    /**
     * Creates a retry interceptor with exponential backoff.
     *
     * @param maxRetries Maximum number of retries
     * @param initialDelayMs Initial delay in milliseconds
     * @param backoffMultiplier Multiplier for each subsequent retry (e.g., 2.0 for doubling)
     */
    public RetryInterceptor(int maxRetries, long initialDelayMs, double backoffMultiplier) {
        this(maxRetries, initialDelayMs, backoffMultiplier, 30000, null);
    }

    /**
     * Creates a fully customized retry interceptor.
     *
     * @param maxRetries Maximum number of retries
     * @param initialDelayMs Initial delay in milliseconds
     * @param backoffMultiplier Multiplier for exponential backoff
     * @param maxDelayMs Maximum delay cap in milliseconds
     * @param retryPredicate Custom predicate to determine if exception is retryable (null for default)
     */
    public RetryInterceptor(int maxRetries, long initialDelayMs, double backoffMultiplier,
                           long maxDelayMs, Predicate<RocketRestException> retryPredicate) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        if (initialDelayMs < 0) {
            throw new IllegalArgumentException("initialDelayMs must be non-negative");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }

        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMs = maxDelayMs;
        this.retryPredicate = retryPredicate != null ? retryPredicate : createDefaultPredicate();
    }

    @Override
    public <Req, Res> Res onError(RocketRestException e, RequestSpec<Req, Res> request,
                                   InterceptorChain chain) throws RocketRestException {
        int currentRetry = chain.getRetryCount();

        // Check if we should retry
        if (currentRetry >= maxRetries) {
            logger.debug("Max retries ({}) exceeded for {} {}",
                maxRetries, request.getMethod(), request.getEndpoint());
            throw e;
        }

        if (!retryPredicate.test(e)) {
            logger.debug("Exception not retryable: {} (status {})",
                e.getClass().getSimpleName(), e.getStatusCode());
            throw e;
        }

        // Calculate delay with exponential backoff
        long delay = calculateDelay(currentRetry);

        logger.info("Retrying request {} {} (attempt {}/{}) after {}ms due to: {}",
            request.getMethod(), request.getEndpoint(),
            currentRetry + 1, maxRetries, delay, e.getMessage());

        // Sleep before retry
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RocketRestException("Retry interrupted", ie);
            }
        }

        // Retry the request
        return chain.retry(request);
    }

    @Override
    public int getOrder() {
        return 100; // Run after most interceptors, before logging
    }

    /**
     * Calculates the delay for a given retry attempt using exponential backoff.
     */
    private long calculateDelay(int retryCount) {
        double delay = initialDelayMs * Math.pow(backoffMultiplier, retryCount);
        return Math.min((long) delay, maxDelayMs);
    }

    /**
     * Creates the default retry predicate.
     */
    private Predicate<RocketRestException> createDefaultPredicate() {
        return new Predicate<RocketRestException>() {
            @Override
            public boolean test(RocketRestException e) {
                // Never retry circuit breaker exceptions
                if (e instanceof CircuitBreakerOpenException) {
                    return false;
                }

                int statusCode = e.getStatusCode();

                // Retry on connection errors (no status code)
                if (statusCode <= 0) {
                    return true;
                }

                // Retry on specific status codes
                if (DEFAULT_RETRYABLE_STATUS_CODES.contains(statusCode)) {
                    return true;
                }

                // Retry on any 5xx error
                if (statusCode >= HttpConstants.StatusCodes.SERVER_ERROR_MIN &&
                    statusCode <= HttpConstants.StatusCodes.SERVER_ERROR_MAX) {
                    return true;
                }

                return false;
            }
        };
    }

    /**
     * Creates a builder for custom retry configuration.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating customized RetryInterceptor instances.
     */
    public static class Builder {
        private int maxRetries = 3;
        private long initialDelayMs = 1000;
        private double backoffMultiplier = 2.0;
        private long maxDelayMs = 30000;
        private Predicate<RocketRestException> retryPredicate;

        /**
         * Sets the maximum number of retries.
         *
         * @param maxRetries Maximum retries (must be non-negative)
         * @return This builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the initial delay between retries.
         *
         * @param initialDelayMs Delay in milliseconds
         * @return This builder
         */
        public Builder initialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
            return this;
        }

        /**
         * Sets the backoff multiplier for exponential backoff.
         *
         * @param multiplier Multiplier (must be >= 1.0)
         * @return This builder
         */
        public Builder backoffMultiplier(double multiplier) {
            this.backoffMultiplier = multiplier;
            return this;
        }

        /**
         * Sets the maximum delay cap.
         *
         * @param maxDelayMs Maximum delay in milliseconds
         * @return This builder
         */
        public Builder maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        /**
         * Sets a custom predicate to determine if an exception should trigger retry.
         *
         * @param predicate The retry predicate
         * @return This builder
         */
        public Builder retryOn(Predicate<RocketRestException> predicate) {
            this.retryPredicate = predicate;
            return this;
        }

        /**
         * Builds the RetryInterceptor with configured settings.
         *
         * @return A new RetryInterceptor instance
         */
        public RetryInterceptor build() {
            return new RetryInterceptor(maxRetries, initialDelayMs, backoffMultiplier,
                maxDelayMs, retryPredicate);
        }
    }
}
