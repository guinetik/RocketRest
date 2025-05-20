package com.guinetik.rr.api;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.auth.TokenExpiredException;
import com.guinetik.rr.http.RocketClient;
import com.guinetik.rr.http.RocketHeaders;
import com.guinetik.rr.http.RocketRestException;
import com.guinetik.rr.request.RequestSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Abstract base class for API clients that provides common HTTP request handling.
 * This class is agnostic to the actual HTTP client implementation.
 */
public abstract class AbstractApiClient {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected String baseUrl;
    protected final RocketRestConfig config;
    protected final RocketRestOptions options;
    protected RocketClient httpClient;

    /**
     * Creates a new API client.
     *
     * @param baseUrl    The base URL for API requests.
     * @param config     The RocketRest configuration.
     * @param httpClient The HTTP client implementation to use.
     */
    protected AbstractApiClient(String baseUrl, RocketRestConfig config, RocketClient httpClient) {
        this.baseUrl = baseUrl;
        this.config = config;
        this.httpClient = httpClient;

        // Initialize options with defaults from config if available
        if (config != null && config.getDefaultOptions() != null) {
            // Create a new ClientOptions and copy values from the config's default options
            this.options = new RocketRestOptions();
            RocketRestOptions defaultOptions = config.getDefaultOptions();

            // Copy all default options to this client's options
            for (String key : defaultOptions.getKeys()) {
                Object value = defaultOptions.getRaw(key);
                if (value != null) {
                    this.options.set(key, value);
                }
            }

            logger.debug("Initialized client with default options from config");
        } else {
            this.options = new RocketRestOptions();
        }
    }

    /**
     * Method to handle token refresh.
     * Refreshes the authentication token if needed using the configured auth strategy.
     */
    protected void refreshToken() {
        if (config != null && config.getAuthStrategy() != null && config.getAuthStrategy().needsTokenRefresh()) {
            logger.debug("Refreshing authentication token");
            boolean refreshed = config.getAuthStrategy().refreshCredentials();
            if (refreshed) {
                logger.debug("Credentials refreshed successfully");
            } else {
                logger.warn("Credential refresh failed");
            }
        }
    }


    /**
     * Sets a client option value.
     *
     * @param key   The option key.
     * @param value The option value.
     * @return This client instance for method chaining.
     */
    public AbstractApiClient configure(String key, Object value) {
        options.set(key, value);
        logger.debug("Set configuration option: {} = {}", key, value);
        return this;
    }

    /**
     * Executes the given request specification and returns the response.
     *
     * @param <Req>       The type of the request.
     * @param <Res>       The type of the response.
     * @param requestSpec The request specification.
     * @return The response object.
     */
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) {
        if (options.getBoolean(RocketRestOptions.TIMING_ENABLED, true)) {
            return executeWithTiming(requestSpec);
        }
        return executeWithRetry(requestSpec, options.getInt(RocketRestOptions.MAX_RETRIES, 1));
    }

    /**
     * Executes a request with timing measurement if enabled.
     *
     * @param <Req>       The type of the request.
     * @param <Res>       The type of the response.
     * @param requestSpec The specification of the request to be executed.
     * @return The response object.
     */
    protected <Req, Res> Res executeWithTiming(RequestSpec<Req, Res> requestSpec) {
        long startTime = System.currentTimeMillis();
        try {
            return executeWithRetry(requestSpec, options.getInt(RocketRestOptions.MAX_RETRIES, 1));
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (options.getBoolean(RocketRestOptions.LOGGING_ENABLED, true)) {
                logger.info("Request completed in {}ms: {} {}", duration,
                        requestSpec.getMethod(), requestSpec.getEndpoint());
            }
        }
    }

    /**
     * Executes an API request with retry logic. If the request fails due to a token expiration,
     * it will attempt to refresh the token and retry the request up to a maximum number of retries.
     *
     * @param <Req>       The type of the request.
     * @param <Res>       The type of the response.
     * @param requestSpec The specification of the request to be executed.
     * @param retriesLeft The number of retries remaining.
     * @return The response object.
     * @throws ApiException if the request fails after all retries are exhausted.
     */
    protected <Req, Res> Res executeWithRetry(RequestSpec<Req, Res> requestSpec, int retriesLeft) {
        try {
            // If logging is enabled, log the request
            if (options.getBoolean(RocketRestOptions.LOGGING_ENABLED, true)) {
                logRequest(requestSpec);
            }
            // If we have auth headers, apply them early, at the requestspec level
            if(config.getAuthStrategy() != null) {
                config.getAuthStrategy().applyAuthHeaders(requestSpec.getHeaders());
            }
            // Delegate to the HTTP client implementation
            return httpClient.execute(requestSpec);
        } catch (TokenExpiredException e) {
            if (retriesLeft > 0 && options.getBoolean(RocketRestOptions.RETRY_ENABLED, true)) {
                logger.debug("Token expired, attempting refresh. Retries left: {}", retriesLeft);
                refreshToken();
                config.getAuthStrategy().applyAuthHeaders(requestSpec.getHeaders());
                // Apply delay if configured
                long retryDelay = options.getLong(RocketRestOptions.RETRY_DELAY, 0);
                if (retryDelay > 0) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ApiException("Retry interrupted", ie);
                    }
                }
                return executeWithRetry(requestSpec, retriesLeft - 1);
            }
            logger.error("Token refresh failed after maximum retries");
            throw new ApiException("Token refresh failed after maximum retries", e);
        } catch (com.guinetik.rr.http.CircuitBreakerOpenException e) {
            // Don't wrap CircuitBreakerOpenException, just rethrow it
            logger.warn("Circuit breaker is open", e);
            throw e;
        } catch (RocketRestException e) {
            logger.error("HTTP client error", e);
            throw new ApiException("Failed to execute request", e);
        }
    }

    /**
     * Logs information about the request if logging is enabled.
     *
     * @param <Req>       The type of the request.
     * @param <Res>       The type of the response.
     * @param requestSpec The request specification.
     */
    protected <Req, Res> void logRequest(RequestSpec<Req, Res> requestSpec) {
        logger.info("Executing {} request to: {}", requestSpec.getMethod(), requestSpec.getEndpoint());

        if (options.getBoolean(RocketRestOptions.LOG_REQUEST_BODY, false) && requestSpec.getBody() != null) {
            // In real implementation, should be careful about logging sensitive information
            logger.debug("Request body: {}", requestSpec.getBody());
        }
    }

    /**
     * Creates a map of HTTP headers, including default headers, authorization headers,
     * and any custom headers provided.
     *
     * @param customHeaders A map of custom headers to include in the request.
     * @return A complete map of headers for the request.
     */
    protected RocketHeaders createHeaders(Map<String, String> customHeaders) {
        // Start with default JSON headers
        RocketHeaders headers = RocketHeaders.defaultJson();

        // Add auth headers using the auth strategy
        if (config != null && config.getAuthStrategy() != null) {
            // Use the newer applyAuthHeaders method which has a better implementation
            headers = config.getAuthStrategy().applyAuthHeaders(headers);
        }

        // Add custom headers
        if (customHeaders != null) {
            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                headers.set(entry.getKey(), entry.getValue());
            }
        }

        return headers;
    }


    public void setBaseUrl(String baseUrl){
        this.baseUrl = baseUrl;
        this.config.setServiceUrl(baseUrl);
        this.httpClient.setBaseUrl(baseUrl);
    }
}
