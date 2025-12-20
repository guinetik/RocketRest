package com.guinetik.rr.http;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.auth.RocketSSL;
import com.guinetik.rr.interceptor.InterceptingClient;
import com.guinetik.rr.interceptor.RequestInterceptor;
import com.guinetik.rr.interceptor.RetryInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Factory for creating and configuring HTTP clients with decorators and settings.
 *
 * <p>This factory provides a fluent builder API for constructing {@link RocketClient}
 * instances with various configurations like circuit breakers, custom decorators,
 * and async execution support.
 *
 * <h2>Simple Client</h2>
 * <pre class="language-java"><code>
 * RocketClient client = RocketClientFactory.builder("https://api.example.com")
 *     .build();
 * </code></pre>
 *
 * <h2>With Circuit Breaker</h2>
 * <pre class="language-java"><code>
 * RocketClient client = RocketClientFactory.builder("https://api.example.com")
 *     .withCircuitBreaker(5, 30000)  // 5 failures, 30s timeout
 *     .build();
 * </code></pre>
 *
 * <h2>From Configuration</h2>
 * <pre class="language-java"><code>
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .authStrategy(AuthStrategyFactory.createBearerToken("token"))
 *     .build();
 *
 * RocketClient client = RocketClientFactory.fromConfig(config)
 *     .withCircuitBreaker()
 *     .build();
 * </code></pre>
 *
 * <h2>Async Client</h2>
 * <pre class="language-java"><code>
 * ExecutorService executor = Executors.newFixedThreadPool(4);
 *
 * AsyncHttpClient asyncClient = RocketClientFactory.builder("https://api.example.com")
 *     .withExecutorService(executor)
 *     .buildAsync();
 * </code></pre>
 *
 * <h2>Fluent Client (Result Pattern)</h2>
 * <pre class="language-java"><code>
 * FluentHttpClient fluentClient = RocketClientFactory.builder("https://api.example.com")
 *     .buildFluent();
 *
 * Result&lt;User, ApiError&gt; result = fluentClient.executeWithResult(request);
 * </code></pre>
 *
 * <h2>Custom Decorator</h2>
 * <pre class="language-java"><code>
 * RocketClient client = RocketClientFactory.builder("https://api.example.com")
 *     .withCircuitBreaker()
 *     .withCustomDecorator(base -&gt; new LoggingClientDecorator(base))
 *     .build();
 * </code></pre>
 *
 * <h2>With Interceptors</h2>
 * <pre class="language-java">{@code
 * // Add retry with exponential backoff
 * RocketClient client = RocketClientFactory.builder("https://api.example.com")
 *     .withRetry(3, 1000)  // 3 retries, 1s initial delay
 *     .build();
 *
 * // Multiple interceptors
 * RocketClient client = RocketClientFactory.builder("https://api.example.com")
 *     .withInterceptor(new LoggingInterceptor())
 *     .withRetry(3, 1000, 2.0)  // With exponential backoff
 *     .build();
 * }</pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketClient
 * @see CircuitBreakerClient
 * @see FluentHttpClient
 * @see RequestInterceptor
 * @see RetryInterceptor
 * @since 1.0.0
 */
public class RocketClientFactory {

    // Function type for client provider used in testing/mocking
    private static UnaryOperator<RocketRestConfig> clientProvider = null;

    /**
     * Sets a custom client provider for testing/mocking.
     * This allows tests to inject mock clients.
     * 
     * @param provider The provider function
     */
    public static void setClientProvider(UnaryOperator<RocketRestConfig> provider) {
        clientProvider = provider;
    }
    
    /**
     * Resets the client provider to the default.
     */
    public static void resetToDefault() {
        clientProvider = null;
    }

    /**
     * Builder class for constructing RocketClient instances with various decorators.
     */
    public static class Builder {
        private final String baseUrl;
        private RocketRestOptions options;
        private ExecutorService executorService;
        private boolean enableCircuitBreaker = false;
        private int failureThreshold = HttpConstants.CircuitBreaker.DEFAULT_FAILURE_THRESHOLD;
        private long resetTimeoutMs = HttpConstants.CircuitBreaker.DEFAULT_RESET_TIMEOUT_MS;
        private long failureDecayTimeMs = HttpConstants.CircuitBreaker.DEFAULT_FAILURE_DECAY_TIME_MS;
        private CircuitBreakerClient.FailurePolicy failurePolicy = CircuitBreakerClient.FailurePolicy.ALL_EXCEPTIONS;
        private Predicate<RocketRestException> failurePredicate = null;
        private UnaryOperator<RocketClient> customDecorator = null;
        private List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        private int interceptorMaxRetries = 3;

        private Builder(String baseUrl) {
            this.baseUrl = baseUrl;
            this.options = new RocketRestOptions();
        }

        /**
         * Sets the client options.
         *
         * @param options The RocketRestOptions to use
         * @return this builder instance
         */
        public Builder withOptions(RocketRestOptions options) {
            this.options = options;
            return this;
        }

        /**
         * Sets the executor service for async operations.
         *
         * @param executorService The executor service to use
         * @return this builder instance
         */
        public Builder withExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        /**
         * Enables the circuit breaker pattern with default settings.
         *
         * @return this builder instance
         */
        public Builder withCircuitBreaker() {
            this.enableCircuitBreaker = true;
            return this;
        }

        /**
         * Enables the circuit breaker pattern with custom settings.
         *
         * @param failureThreshold Number of failures before opening circuit
         * @param resetTimeoutMs   Timeout in ms before trying to close the circuit
         * @return this builder instance
         */
        public Builder withCircuitBreaker(int failureThreshold, long resetTimeoutMs) {
            this.enableCircuitBreaker = true;
            this.failureThreshold = failureThreshold;
            this.resetTimeoutMs = resetTimeoutMs;
            return this;
        }

        /**
         * Enables the circuit breaker pattern with fully customized settings.
         *
         * @param failureThreshold   Number of failures before opening circuit
         * @param resetTimeoutMs     Timeout in ms before trying to close the circuit
         * @param failureDecayTimeMs Time after which failure count starts to decay
         * @param failurePolicy      Strategy to determine what counts as a failure
         * @return this builder instance
         */
        public Builder withCircuitBreaker(int failureThreshold, long resetTimeoutMs,
                                          long failureDecayTimeMs, CircuitBreakerClient.FailurePolicy failurePolicy) {
            this.enableCircuitBreaker = true;
            this.failureThreshold = failureThreshold;
            this.resetTimeoutMs = resetTimeoutMs;
            this.failureDecayTimeMs = failureDecayTimeMs;
            this.failurePolicy = failurePolicy;
            return this;
        }

        /**
         * Sets a custom failure predicate for the circuit breaker.
         *
         * @param failurePredicate Custom predicate to determine what counts as a failure
         * @return this builder instance
         */
        public Builder withFailurePredicate(Predicate<RocketRestException> failurePredicate) {
            this.failurePredicate = failurePredicate;
            return this;
        }

        /**
         * Adds a custom decorator function that will be applied to the client.
         *
         * @param decorator Function that takes a client and returns a decorated client
         * @return this builder instance
         */
        public Builder withCustomDecorator(UnaryOperator<RocketClient> decorator) {
            this.customDecorator = decorator;
            return this;
        }

        /**
         * Adds an interceptor to the client.
         *
         * <p>Interceptors are applied in order based on their {@link RequestInterceptor#getOrder()}.
         * Lower order values run first for requests and last for responses.
         *
         * @param interceptor The interceptor to add
         * @return this builder instance
         * @see RequestInterceptor
         */
        public Builder withInterceptor(RequestInterceptor interceptor) {
            if (interceptor != null) {
                this.interceptors.add(interceptor);
            }
            return this;
        }

        /**
         * Adds retry capability with default settings.
         *
         * <p>Uses 3 retries with 1 second initial delay, 2x exponential backoff,
         * and 30 second maximum delay.
         *
         * @return this builder instance
         */
        public Builder withRetry() {
            return withInterceptor(new RetryInterceptor());
        }

        /**
         * Adds retry capability with custom retry count and delay.
         *
         * @param maxRetries Maximum number of retries
         * @param initialDelayMs Initial delay between retries in milliseconds
         * @return this builder instance
         */
        public Builder withRetry(int maxRetries, long initialDelayMs) {
            return withInterceptor(new RetryInterceptor(maxRetries, initialDelayMs));
        }

        /**
         * Adds retry capability with exponential backoff.
         *
         * @param maxRetries Maximum number of retries
         * @param initialDelayMs Initial delay in milliseconds
         * @param backoffMultiplier Multiplier for each retry (e.g., 2.0 doubles delay)
         * @return this builder instance
         */
        public Builder withRetry(int maxRetries, long initialDelayMs, double backoffMultiplier) {
            return withInterceptor(new RetryInterceptor(maxRetries, initialDelayMs, backoffMultiplier));
        }

        /**
         * Sets the maximum number of retries allowed by the interceptor chain.
         *
         * <p>This is a global limit that applies across all retry interceptors.
         * Default is 3.
         *
         * @param maxRetries Maximum retries for the interceptor chain
         * @return this builder instance
         */
        public Builder withMaxRetries(int maxRetries) {
            this.interceptorMaxRetries = maxRetries;
            return this;
        }

        /**
         * Builds a synchronous RocketClient with the configured settings.
         *
         * @return A new RocketClient instance
         */
        public RocketClient build() {
            // First check if there's a custom client provider for testing
            if (clientProvider != null) {
                RocketRestConfig config = new RocketRestConfig.Builder(baseUrl)
                    .defaultOptions(o -> {
                        for (String key : options.getKeys()) {
                            o.set(key, options.getRaw(key));
                        }
                    })
                    .build();
                return (RocketClient) clientProvider.apply(config);
            }
            // Check if the options have circuit breaker enabled
            boolean circuitBreakerEnabled = this.enableCircuitBreaker || 
                    options.getBoolean(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_ENABLED, false);
            // Create base client
            RocketClient client = new DefaultHttpClient(baseUrl, options);
            
            // Apply circuit breaker if enabled in builder or options
            if (circuitBreakerEnabled) {
                // Get circuit breaker settings from options if not specified in builder
                int threshold = this.enableCircuitBreaker ? 
                        this.failureThreshold : 
                        options.getInt(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_THRESHOLD, 
                                HttpConstants.CircuitBreaker.DEFAULT_FAILURE_THRESHOLD);
                // 
                long timeout = this.enableCircuitBreaker ? 
                        this.resetTimeoutMs : 
                        options.getLong(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_RESET_TIMEOUT_MS, 
                                HttpConstants.CircuitBreaker.DEFAULT_RESET_TIMEOUT_MS);
                // Determine failure policy
                CircuitBreakerClient.FailurePolicy policy = this.failurePolicy;
                if (!this.enableCircuitBreaker && options.contains(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_POLICY)) {
                    String policyStr = options.getString(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_POLICY, null);
                    if (HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_POLICY_SERVER_ONLY.equals(policyStr)) {
                        policy = CircuitBreakerClient.FailurePolicy.SERVER_ERRORS_ONLY;
                    }
                }
                // Apply circuit breaker
                client = new CircuitBreakerClient(
                        client,
                        threshold,
                        timeout,
                        this.failureDecayTimeMs,
                        policy,
                        this.failurePredicate
                );
            }
            // Apply interceptors if any are configured
            if (!interceptors.isEmpty()) {
                client = new InterceptingClient(client, interceptors, interceptorMaxRetries);
            }

            // Apply any custom decorator
            if (customDecorator != null) {
                client = customDecorator.apply(client);
            }
            // Return the client
            return client;
        }

        /**
         * Builds a fluent HTTP client with the configured settings.
         * This client uses the Result pattern instead of exceptions.
         *
         * @return A new FluentHttpClient instance
         */
        public FluentHttpClient buildFluent() {
            RocketClient client = build();
            return new FluentHttpClient(client, baseUrl, options);
        }

        /**
         * Builds an asynchronous RocketClient with the configured settings.
         *
         * @return A new AsyncHttpClient instance
         * @throws IllegalStateException if no executor service was provided
         */
        public AsyncHttpClient buildAsync() {
            if (executorService == null) {
                throw new IllegalStateException("ExecutorService must be provided for async client");
            }

            RocketClient client = build();
            return new AsyncHttpClient(client, executorService);
        }
    }

    /**
     * Creates a builder for constructing RocketClient instances.
     *
     * @param baseUrl The base URL for the client
     * @return A new builder instance
     */
    public static Builder builder(String baseUrl) {
        return new Builder(baseUrl);
    }

    /**
     * Creates a builder from an existing RocketRestConfig.
     *
     * @param config The RocketRestConfig to use
     * @return A new builder instance pre-configured with settings from the config
     */
    public static Builder fromConfig(RocketRestConfig config) {
        return builder(config.getServiceUrl())
                .withOptions(config.getDefaultOptions());
    }

    /**
     * Creates a default HTTP client with the given config.
     *
     * @param config The RocketRestConfig to use
     * @return A new DefaultHttpClient instance
     */
    public static RocketClient createDefaultClient(RocketRestConfig config) {
        return builder(config.getServiceUrl())
                .withOptions(config.getDefaultOptions())
                .build();
    }

    /**
     * Creates a fluent HTTP client with the given config.
     * This client uses the Result pattern instead of exceptions.
     *
     * @param config The RocketRestConfig to use
     * @return A new FluentHttpClient instance
     */
    public static FluentHttpClient createFluentClient(RocketRestConfig config) {
        return new FluentHttpClient(config.getServiceUrl(), config.getDefaultOptions());
    }

    /**
     * Creates a fluent HTTP client with the given base URL.
     * This client uses the Result pattern instead of exceptions.
     *
     * @param baseUrl The base URL for the client
     * @return A new FluentHttpClient instance
     */
    public static FluentHttpClient createFluentClient(String baseUrl) {
        return new FluentHttpClient(baseUrl);
    }
} 