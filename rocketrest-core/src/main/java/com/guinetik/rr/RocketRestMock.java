package com.guinetik.rr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guinetik.rr.api.ApiException;
import com.guinetik.rr.http.*;
import com.guinetik.rr.request.RequestSpec;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Mock implementation of {@link RocketRest} for unit testing without actual HTTP requests.
 *
 * <p>This class simulates REST API interactions by returning predefined responses based on
 * HTTP method and URL patterns. It supports regex matching, simulated network latency,
 * invocation counting, and circuit breaker testing.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Predefined mock responses for any HTTP method and URL pattern</li>
 *   <li>Regex-based URL matching for flexible endpoint simulation</li>
 *   <li>Simulated network latency for timing-sensitive tests</li>
 *   <li>Invocation counting for verification in tests</li>
 *   <li>Circuit breaker integration for resilience testing</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre class="language-java"><code>
 * // Create mock client
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com").build();
 * RocketRestMock mockClient = new RocketRestMock(config);
 *
 * // Define mock responses
 * mockClient.addMockResponse("GET", "/users/1", (url, body) -&gt; {
 *     User user = new User();
 *     user.setId(1);
 *     user.setName("John Doe");
 *     return user;
 * });
 *
 * // Use in tests
 * User user = mockClient.get("/users/1", User.class);
 * assertEquals("John Doe", user.getName());
 * </code></pre>
 *
 * <h2>Regex URL Matching</h2>
 * <pre class="language-java">{@code
 * // Match any user ID (use regex pattern like /users/[0-9]+)
 * mockClient.addMockResponse("GET", "/users/[0-9]+", (url, body) -> {
 *     // Extract ID from URL and return corresponding user
 *     String id = url.substring(url.lastIndexOf('/') + 1);
 *     User user = new User();
 *     user.setId(Integer.parseInt(id));
 *     return user;
 * }, true);  // true enables regex matching
 * }</pre>
 *
 * <h2>Simulating Latency</h2>
 * <pre class="language-java"><code>
 * // Add 500ms latency for slow endpoint testing
 * mockClient.withLatency("/slow-endpoint.*", 500L);
 *
 * // Test timeout behavior
 * Result&lt;Response, ApiError&gt; result = mockClient.fluent()
 *     .get("/slow-endpoint", Response.class);
 * </code></pre>
 *
 * <h2>Verifying Invocations</h2>
 * <pre class="language-java"><code>
 * // Make some calls
 * mockClient.get("/users/1", User.class);
 * mockClient.get("/users/1", User.class);
 *
 * // Verify call count
 * assertEquals(2, mockClient.getInvocationCount("GET", "/users/1"));
 *
 * // Reset for next test
 * mockClient.resetInvocationCounts();
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketRest
 * @see com.guinetik.rr.http.MockRocketClient
 * @since 1.0.0
 */
public class RocketRestMock extends RocketRest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map to store mock responses: key is "METHOD:url", value is response producer
    private final Map<String, MockResponseDefinition> mockResponses = new HashMap<>();
    
    // Map to track invocation counts: key is "METHOD:url", value is count
    private final ConcurrentHashMap<String, Integer> invocationCounts = new ConcurrentHashMap<>();
    
    // Map to store latency settings: key is url pattern regex, value is latency in ms
    private final Map<Pattern, Long> latencySettings = new HashMap<>();

    // The mock client implementation
    private final MockRocketClient mockClient;

    /**
     * Creates a new mock client instance.
     *
     * @param config the configuration for the REST client
     */
    public RocketRestMock(RocketRestConfig config) {
        super(config);
        
        // Create the base mock client
        this.mockClient = new MockRocketClient();
        
        // If circuit breaker is enabled, wrap the mock client with CircuitBreakerClient
        if (config.getDefaultOptions().getBoolean(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_ENABLED, false)) {
            int failureThreshold = config.getDefaultOptions().getInt(
                HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_THRESHOLD,
                HttpConstants.CircuitBreaker.DEFAULT_FAILURE_THRESHOLD
            );
            long resetTimeoutMs = config.getDefaultOptions().getLong(
                HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_RESET_TIMEOUT_MS,
                HttpConstants.CircuitBreaker.DEFAULT_RESET_TIMEOUT_MS
            );
            
            // Create a circuit breaker client with the mock client
            CircuitBreakerClient circuitBreakerClient;
            
            // Set the circuit breaker policy if specified
            String policyStr = config.getDefaultOptions().getString(
                HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_POLICY,
                null
            );
            if (HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_POLICY_SERVER_ONLY.equals(policyStr)) {
                circuitBreakerClient = new CircuitBreakerClient(
                    mockClient,
                    failureThreshold,
                    resetTimeoutMs,
                    HttpConstants.CircuitBreaker.DEFAULT_FAILURE_DECAY_TIME_MS,
                    CircuitBreakerClient.FailurePolicy.SERVER_ERRORS_ONLY,
                    null
                );
            } else {
                circuitBreakerClient = new CircuitBreakerClient(
                    mockClient,
                    failureThreshold,
                    resetTimeoutMs
                );
            }
            
            // Set the circuit breaker client as the delegate
            this.mockClient.setDelegate(circuitBreakerClient);
        }
    }

    /**
     * Mock response definition class that holds both the response producer and whether 
     * the URL pattern should be treated as a regex pattern.
     */
    private static class MockResponseDefinition {
        final BiFunction<String, Object, Object> responseProducer;
        final boolean isRegexPattern;
        final Pattern compiledPattern;
        
        MockResponseDefinition(BiFunction<String, Object, Object> responseProducer, boolean isRegexPattern, String pattern) {
            this.responseProducer = responseProducer;
            this.isRegexPattern = isRegexPattern;
            this.compiledPattern = isRegexPattern ? Pattern.compile(pattern) : null;
        }
    }

    /**
     * Adds a mock response for a specific HTTP method and exact URL match.
     *
     * @param method           HTTP method (GET, POST, PUT, DELETE)
     * @param urlPattern       URL pattern to match exactly
     * @param responseProducer Function that takes (url, requestBody) and returns a response object
     */
    public void addMockResponse(String method, String urlPattern,
                                BiFunction<String, Object, Object> responseProducer) {
        addMockResponse(method, urlPattern, responseProducer, false);
    }

    /**
     * Adds a mock response with the option to use regex pattern matching for URLs.
     *
     * @param method           HTTP method (GET, POST, PUT, DELETE)
     * @param urlPattern       URL pattern (can be a regex pattern if isRegexPattern is true)
     * @param responseProducer Function that takes (url, requestBody) and returns a response object
     * @param isRegexPattern   If true, the urlPattern will be treated as a regex pattern
     */
    public void addMockResponse(String method, String urlPattern,
                                BiFunction<String, Object, Object> responseProducer,
                                boolean isRegexPattern) {
        String key = method + ":" + urlPattern;
        mockResponses.put(key, new MockResponseDefinition(responseProducer, isRegexPattern, urlPattern));
        logger.debug("Added mock response for {} {} (regex: {})", method, urlPattern, isRegexPattern);
    }

    /**
     * Finds a matching mock response producer for the given method and URL.
     * Checks for exact matches first, then tries regex pattern matching.
     */
    private BiFunction<String, Object, Object> findMatchingResponse(String method, String url) {
        // Build the key for direct lookup
        String exactKey = method + ":" + url;
        
        // Try direct mapping first for performance
        MockResponseDefinition exactMatch = mockResponses.get(exactKey);
        if (exactMatch != null) {
            // Track the invocation
            trackInvocation(method, url);
            return exactMatch.responseProducer;
        }
        
        // If no direct match, try regex matches
        for (Map.Entry<String, MockResponseDefinition> entry : mockResponses.entrySet()) {
            String key = entry.getKey();
            MockResponseDefinition def = entry.getValue();
            
            // Skip if not a regex pattern or if the method doesn't match
            if (!def.isRegexPattern || !key.startsWith(method + ":")) {
                continue;
            }
            
            // Check if the URL matches the regex pattern
            Matcher matcher = def.compiledPattern.matcher(url);
            if (matcher.matches()) {
                // Track the invocation using the pattern as the key
                trackInvocation(method, key.substring(method.length() + 1));
                return def.responseProducer;
            }
        }
        
        return null;
    }
    
    /**
     * Tracks an invocation of an endpoint for testing verification.
     */
    private void trackInvocation(String method, String urlPattern) {
        String key = method + ":" + urlPattern;
        invocationCounts.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
    }
    
    /**
     * Gets the number of times an endpoint was called.
     * 
     * @param method HTTP method (GET, POST, PUT, DELETE)
     * @param urlPattern URL pattern or exact URL
     * @return The number of invocations, or 0 if never called
     */
    public int getInvocationCount(String method, String urlPattern) {
        String key = method + ":" + urlPattern;
        return invocationCounts.getOrDefault(key, 0);
    }
    
    /**
     * Resets all invocation counters.
     */
    public void resetInvocationCounts() {
        invocationCounts.clear();
    }
    
    /**
     * Adds simulated network latency for a specific URL pattern.
     * 
     * @param urlPatternRegex Regex pattern for matching URLs
     * @param latencyMs Delay in milliseconds
     */
    public void withLatency(String urlPatternRegex, long latencyMs) {
        latencySettings.put(Pattern.compile(urlPatternRegex), latencyMs);
        logger.debug("Added latency of {}ms for URL pattern: {}", latencyMs, urlPatternRegex);
    }
    
    /**
     * Simulates network latency if configured for the given URL.
     */
    private void simulateLatency(String url) {
        for (Map.Entry<Pattern, Long> entry : latencySettings.entrySet()) {
            if (entry.getKey().matcher(url).matches()) {
                long latency = entry.getValue();
                logger.debug("Simulating latency of {}ms for URL: {}", latency, url);
                try {
                    Thread.sleep(latency);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return; // Only apply the first matching latency
            }
        }
    }

    @Override
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) {
        // Simulate network latency if configured
        simulateLatency(requestSpec.getEndpoint());
        
        // Execute using the mock client (which may be wrapped with CircuitBreakerClient)
        return mockClient.execute(requestSpec);
    }

    /**
     * Helper method to execute a request and wrap the result in a Result object.
     */
    private <Req, Res> Result<Res, ApiError> executeWithResult(RequestSpec<Req, Res> requestSpec) {
        try {
            Res result = execute(requestSpec);
            return Result.success(result);
        } catch (CircuitBreakerOpenException e) {
            return Result.failure(ApiError.circuitOpenError(e.getMessage()));
        } catch (RocketRestException e) {
            return Result.failure(ApiError.httpError(e.getMessage(), e.getStatusCode(), e.getResponseBody()));
        } catch (Exception e) {
            return Result.failure(ApiError.networkError(e.getMessage()));
        }
    }
    
    /**
     * Provides a mock implementation for sync API calls.
     */
    @Override
    public SyncApi sync() {
        return new MockSyncApi();
    }
    
    /**
     * Provides a mock implementation for async API calls.
     */
    @Override
    public AsyncApi async() {
        return new MockAsyncApi();
    }
    
    /**
     * Provides a mock implementation for fluent API calls.
     */
    public FluentApi fluent() {
        return new MockFluentApi();
    }

    public <Req, Res> CompletableFuture<Res> executeAsync(RequestSpec<Req, Res> requestSpec) {
        return CompletableFuture.supplyAsync(() -> execute(requestSpec));
    }

    /**
     * Mock implementation of RocketClient that handles the actual request execution.
     */
    private class MockRocketClient implements RocketClient {
        private RocketClient delegate;
        private boolean isExecuting = false;

        public void setDelegate(RocketClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
            // If we have a delegate, and we're not yet executing, use it
            if (delegate != null && !isExecuting) {
                isExecuting = true;
                try {
                    return delegate.execute(requestSpec);
                } finally {
                    isExecuting = false;
                }
            }

            // Otherwise handle the request directly
            BiFunction<String, Object, Object> responseProducer =
                    findMatchingResponse(requestSpec.getMethod(), requestSpec.getEndpoint());

            if (responseProducer != null) {
                try {
                    Object response = responseProducer.apply(
                            requestSpec.getEndpoint(),
                            requestSpec.getBody()
                    );

                    // Convert the response to the expected type
                    if (requestSpec.getResponseType().isInstance(response)) {
                        @SuppressWarnings("unchecked")
                        Res typedResponse = (Res) response;
                        return typedResponse;
                    } else if (response != null) {
                        // Try to convert using ObjectMapper if types don't match directly
                        return objectMapper.convertValue(response, requestSpec.getResponseType());
                    }

                    throw new ApiException("Mock response could not be converted to required type: "
                            + requestSpec.getResponseType().getName());
                } catch (Exception e) {
                    if (e instanceof ApiException) {
                        ApiException apiEx = (ApiException) e;
                        if (apiEx.getStatusCode() > 0) {
                            throw new RocketRestException(
                                apiEx.getMessage(),
                                apiEx.getStatusCode(),
                                apiEx.getResponseBody()
                            );
                        }
                    }
                    throw new RocketRestException("Failed to process mock response", e);
                }
            }

            logger.warn("No mock response found for {} : {}", requestSpec.getMethod(), requestSpec.getEndpoint());
            throw new RocketRestException("No mock response configured for "
                    + requestSpec.getMethod() + ":" + requestSpec.getEndpoint());
        }

        @Override
        public void configureSsl(javax.net.ssl.SSLContext sslContext) {
            // No-op for a mock client
        }

        @Override
        public void setBaseUrl(String baseUrl) {

        }
    }
    
    // Inner class implementations of the API interfaces
    
    /**
     * Implementation of SyncApi that delegates to the underlying DefaultApiClient.
     */
    private class MockSyncApi implements SyncApi {
        @Override
        public <T> T get(String endpoint, Class<T> responseType) {
            return execute(createGetRequest(endpoint, responseType));
        }
        
        @Override
        public <T> T get(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
            return execute(createGetRequest(endpoint, responseType, queryParams));
        }
        
        @Override
        public <Res> Res post(String endpoint, Class<Res> responseType) {
            return execute(createPostRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Res post(String endpoint, Req body, Class<Res> responseType) {
            return execute(createPostRequest(endpoint, body, responseType));
        }
        
        @Override
        public <Res> Res put(String endpoint, Class<Res> responseType) {
            return execute(createPutRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Res put(String endpoint, Req body, Class<Res> responseType) {
            return execute(createPutRequest(endpoint, body, responseType));
        }
        
        @Override
        public <T> T delete(String endpoint, Class<T> responseType) {
            return execute(createDeleteRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) {
            return RocketRestMock.this.execute(requestSpec);
        }
    }
    
    /**
     * Implementation of AsyncApi that delegates to the underlying AsyncApiClient.
     */
    private class MockAsyncApi implements AsyncApi {
        @Override
        public <T> CompletableFuture<T> get(String endpoint, Class<T> responseType) {
            return executeAsync(createGetRequest(endpoint, responseType));
        }
        
        @Override
        public <T> CompletableFuture<T> get(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
            return executeAsync(createGetRequest(endpoint, responseType, queryParams));
        }
        
        @Override
        public <Res> CompletableFuture<Res> post(String endpoint, Class<Res> responseType) {
            return executeAsync(createPostRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> CompletableFuture<Res> post(String endpoint, Req body, Class<Res> responseType) {
            return executeAsync(createPostRequest(endpoint, body, responseType));
        }
        
        @Override
        public <Res> CompletableFuture<Res> put(String endpoint, Class<Res> responseType) {
            return executeAsync(createPutRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> CompletableFuture<Res> put(String endpoint, Req body, Class<Res> responseType) {
            return executeAsync(createPutRequest(endpoint, body, responseType));
        }
        
        @Override
        public <T> CompletableFuture<T> delete(String endpoint, Class<T> responseType) {
            return executeAsync(createDeleteRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> CompletableFuture<Res> execute(RequestSpec<Req, Res> requestSpec) {
            return executeAsync(requestSpec);
        }
        
        @Override
        public void shutdown() {
            // No-op for mock
        }
    }
    
    /**
     * Implementation of FluentApi that delegates to the underlying FluentApiClient.
     */
    private class MockFluentApi implements FluentApi {
        @Override
        public <T> Result<T, ApiError> get(String endpoint, Class<T> responseType) {
            return RocketRestMock.this.executeWithResult(createGetRequest(endpoint, responseType));
        }
        
        @Override
        public <T> Result<T, ApiError> get(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
            return RocketRestMock.this.executeWithResult(createGetRequest(endpoint, responseType, queryParams));
        }
        
        @Override
        public <Res> Result<Res, ApiError> post(String endpoint, Class<Res> responseType) {
            return RocketRestMock.this.executeWithResult(createPostRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Result<Res, ApiError> post(String endpoint, Req body, Class<Res> responseType) {
            return RocketRestMock.this.executeWithResult(createPostRequest(endpoint, body, responseType));
        }
        
        @Override
        public <Res> Result<Res, ApiError> put(String endpoint, Class<Res> responseType) {
            return RocketRestMock.this.executeWithResult(createPutRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Result<Res, ApiError> put(String endpoint, Req body, Class<Res> responseType) {
            return RocketRestMock.this.executeWithResult(createPutRequest(endpoint, body, responseType));
        }
        
        @Override
        public <T> Result<T, ApiError> delete(String endpoint, Class<T> responseType) {
            return RocketRestMock.this.executeWithResult(createDeleteRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Result<Res, ApiError> execute(RequestSpec<Req, Res> requestSpec) {
            return RocketRestMock.this.executeWithResult(requestSpec);
        }
    }
    
    // Helper methods for creating request specs, reusing parent methods via reflection
    private <T> RequestSpec<Void, T> createGetRequest(String endpoint, Class<T> responseType) {
        try {
            java.lang.reflect.Method method = RocketRest.class.getDeclaredMethod("createGetRequest", String.class, Class.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            RequestSpec<Void, T> result = (RequestSpec<Void, T>) method.invoke(this, endpoint, responseType);
            return result;
        } catch (Exception e) {
            throw new ApiException("Failed to create GET request", e);
        }
    }
    
    private <T> RequestSpec<Void, T> createGetRequest(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
        try {
            java.lang.reflect.Method method = RocketRest.class.getDeclaredMethod("createGetRequest", String.class, Class.class, Map.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            RequestSpec<Void, T> result = (RequestSpec<Void, T>) method.invoke(this, endpoint, responseType, queryParams);
            return result;
        } catch (Exception e) {
            throw new ApiException("Failed to create GET request with params", e);
        }
    }
    
    private <Res> RequestSpec<Void, Res> createPostRequest(String endpoint, Class<Res> responseType) {
        try {
            java.lang.reflect.Method method = RocketRest.class.getDeclaredMethod("createPostRequest", String.class, Class.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            RequestSpec<Void, Res> result = (RequestSpec<Void, Res>) method.invoke(this, endpoint, responseType);
            return result;
        } catch (Exception e) {
            throw new ApiException("Failed to create POST request", e);
        }
    }
    
    private <Req, Res> RequestSpec<Req, Res> createPostRequest(String endpoint, Req body, Class<Res> responseType) {
        try {
            java.lang.reflect.Method method = RocketRest.class.getDeclaredMethod("createPostRequest", String.class, Object.class, Class.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            RequestSpec<Req, Res> result = (RequestSpec<Req, Res>) method.invoke(this, endpoint, body, responseType);
            return result;
        } catch (Exception e) {
            throw new ApiException("Failed to create POST request with body", e);
        }
    }
    
    private <Res> RequestSpec<Void, Res> createPutRequest(String endpoint, Class<Res> responseType) {
        try {
            java.lang.reflect.Method method = RocketRest.class.getDeclaredMethod("createPutRequest", String.class, Class.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            RequestSpec<Void, Res> result = (RequestSpec<Void, Res>) method.invoke(this, endpoint, responseType);
            return result;
        } catch (Exception e) {
            throw new ApiException("Failed to create PUT request", e);
        }
    }
    
    private <Req, Res> RequestSpec<Req, Res> createPutRequest(String endpoint, Req body, Class<Res> responseType) {
        try {
            java.lang.reflect.Method method = RocketRest.class.getDeclaredMethod("createPutRequest", String.class, Object.class, Class.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            RequestSpec<Req, Res> result = (RequestSpec<Req, Res>) method.invoke(this, endpoint, body, responseType);
            return result;
        } catch (Exception e) {
            throw new ApiException("Failed to create PUT request with body", e);
        }
    }
    
    private <T> RequestSpec<Void, T> createDeleteRequest(String endpoint, Class<T> responseType) {
        try {
            java.lang.reflect.Method method = RocketRest.class.getDeclaredMethod("createDeleteRequest", String.class, Class.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            RequestSpec<Void, T> result = (RequestSpec<Void, T>) method.invoke(this, endpoint, responseType);
            return result;
        } catch (Exception e) {
            throw new ApiException("Failed to create DELETE request", e);
        }
    }
}