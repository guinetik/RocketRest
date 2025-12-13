package com.guinetik.rr;

import com.guinetik.rr.api.AsyncApiClient;
import com.guinetik.rr.api.DefaultApiClient;
import com.guinetik.rr.api.FluentApiClient;
import com.guinetik.rr.auth.AbstractOAuth2Strategy;
import com.guinetik.rr.request.RequestBuilder;
import com.guinetik.rr.request.RequestSpec;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main entry point for interacting with REST APIs using RocketRest.
 *
 * <p>This class provides a unified facade for making HTTP requests with three different API styles:
 * <ul>
 *   <li><b>Synchronous API</b> - Traditional blocking calls via {@link #sync()}</li>
 *   <li><b>Asynchronous API</b> - Non-blocking calls with {@link CompletableFuture} via {@link #async()}</li>
 *   <li><b>Fluent API</b> - Functional error handling with {@link Result} pattern via {@link #fluent()}</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre class="language-java"><code>
 * // Create a client with simple URL
 * RocketRest client = new RocketRest("https://api.example.com");
 *
 * // Make a GET request
 * User user = client.get("/users/1", User.class);
 *
 * // Don't forget to shutdown when done
 * client.shutdown();
 * </code></pre>
 *
 * <h2>Using Different API Styles</h2>
 * <pre class="language-java"><code>
 * // Synchronous API - blocks until response
 * Todo todo = client.sync().get("/todos/1", Todo.class);
 *
 * // Asynchronous API - returns CompletableFuture
 * CompletableFuture&lt;Todo&gt; future = client.async().get("/todos/1", Todo.class);
 * future.thenAccept(t -&gt; System.out.println(t.getTitle()));
 *
 * // Fluent API with Result pattern - no exceptions
 * Result&lt;Todo, ApiError&gt; result = client.fluent().get("/todos/1", Todo.class);
 * result.match(
 *     todo -&gt; System.out.println("Success: " + todo.getTitle()),
 *     error -&gt; System.err.println("Error: " + error.getMessage())
 * );
 * </code></pre>
 *
 * <h2>Configuration</h2>
 * <pre class="language-java"><code>
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .authStrategy(AuthStrategyFactory.createBearerToken("my-token"))
 *     .defaultOptions(opts -&gt; {
 *         opts.set(RocketRestOptions.RETRY_ENABLED, true);
 *         opts.set(RocketRestOptions.MAX_RETRIES, 3);
 *     })
 *     .build();
 *
 * RocketRest client = new RocketRest(config);
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketRestConfig
 * @see Result
 * @since 1.0.0
 */
public class RocketRest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RocketRestOptions options;
    private final DefaultApiClient syncClient;
    private final AsyncApiClient asyncClient;
    private final FluentApiClient fluentClient;
    
    /**
     * Interface for synchronous API operations.
     */
    public interface SyncApi {
        <T> T get(String endpoint, Class<T> responseType);
        <T> T get(String endpoint, Class<T> responseType, Map<String, String> queryParams);
        <Res> Res post(String endpoint, Class<Res> responseType);
        <Req, Res> Res post(String endpoint, Req body, Class<Res> responseType);
        <Res> Res put(String endpoint, Class<Res> responseType);
        <Req, Res> Res put(String endpoint, Req body, Class<Res> responseType);
        <T> T delete(String endpoint, Class<T> responseType);
        <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec);
    }

    /**
     * Interface for asynchronous API operations.
     */
    public interface AsyncApi {
        <T> CompletableFuture<T> get(String endpoint, Class<T> responseType);
        <T> CompletableFuture<T> get(String endpoint, Class<T> responseType, Map<String, String> queryParams);
        <Res> CompletableFuture<Res> post(String endpoint, Class<Res> responseType);
        <Req, Res> CompletableFuture<Res> post(String endpoint, Req body, Class<Res> responseType);
        <Res> CompletableFuture<Res> put(String endpoint, Class<Res> responseType);
        <Req, Res> CompletableFuture<Res> put(String endpoint, Req body, Class<Res> responseType);
        <T> CompletableFuture<T> delete(String endpoint, Class<T> responseType);
        <Req, Res> CompletableFuture<Res> execute(RequestSpec<Req, Res> requestSpec);
        void shutdown();
    }

    /**
     * Interface for fluent API operations with a Result pattern.
     */
    public interface FluentApi {
        <T> Result<T, ApiError> get(String endpoint, Class<T> responseType);
        <T> Result<T, ApiError> get(String endpoint, Class<T> responseType, Map<String, String> queryParams);
        <Res> Result<Res, ApiError> post(String endpoint, Class<Res> responseType);
        <Req, Res> Result<Res, ApiError> post(String endpoint, Req body, Class<Res> responseType);
        <Res> Result<Res, ApiError> put(String endpoint, Class<Res> responseType);
        <Req, Res> Result<Res, ApiError> put(String endpoint, Req body, Class<Res> responseType);
        <T> Result<T, ApiError> delete(String endpoint, Class<T> responseType);
        <Req, Res> Result<Res, ApiError> execute(RequestSpec<Req, Res> requestSpec);
    }

    private RocketRestConfig config;

    /**
     * Creates a new {@link RocketRest} instance.
     *
     * @param config the configuration for the REST client.
     */
    public RocketRest(RocketRestConfig config) {
        this(config.getServiceUrl(), config);
    }

    /**
     * Creates a new {@link RocketRest} instance with a specific base URL.
     *
     * @param baseUrl the base URL of the API.
     * @param config  the configuration for the REST client.
     */
    public RocketRest(String baseUrl, RocketRestConfig config) {
        // Initialize options from config's default options
        this.config = config;
        if (this.config != null && this.config.getDefaultOptions() != null) {
            this.options = new RocketRestOptions();
            RocketRestOptions defaultOptions = this.config.getDefaultOptions();

            // Copy all default options to this client's options
            for (String key : defaultOptions.getKeys()) {
                Object value = defaultOptions.getRaw(key);
                if (value != null) {
                    this.options.set(key, value);
                }
            }
        } else {
            this.options = new RocketRestOptions();
        }

        // Initialize clients (they will get default options from config)
        this.syncClient = new DefaultApiClient(baseUrl, this.config);

        // Get the pool size from options
        int poolSize = options.getInt(RocketRestOptions.ASYNC_POOL_SIZE, 4);
        ExecutorService asyncExecutor = Executors.newFixedThreadPool(poolSize);
        this.asyncClient = new AsyncApiClient(baseUrl, this.config, asyncExecutor);
        
        // Initialize the fluent client
        this.fluentClient = new FluentApiClient(baseUrl, this.config);

        logger.info("Initialized RocketRest with base URL: {} and async pool size: {}", baseUrl, poolSize);
    }

    /**
     * Performs a synchronous GET request to the specified endpoint.
     *
     * @param <T> The response type
     * @param endpoint The API endpoint
     * @param responseType The class of the response type
     * @return The response object
     */
    public <T> T get(String endpoint, Class<T> responseType) {
        return sync().get(endpoint, responseType);
    }

    /**
     * Performs a synchronous GET request with query parameters.
     *
     * @param <T> The response type
     * @param endpoint The API endpoint
     * @param responseType The class of the response type
     * @param queryParams The query parameters
     * @return The response object
     */
    public <T> T get(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
        return sync().get(endpoint, responseType, queryParams);
    }

    /**
     * Performs a synchronous POST request.
     *
     * @param <Res> The response type
     * @param endpoint The API endpoint
     * @param responseType The class of the response type
     * @return The response object
     */
    public <Res> Res post(String endpoint, Class<Res> responseType) {
        return sync().post(endpoint, responseType);
    }

    /**
     * Performs a synchronous POST request with a body.
     *
     * @param <Req> The request body type
     * @param <Res> The response type
     * @param endpoint The API endpoint
     * @param body The request body
     * @param responseType The class of the response type
     * @return The response object
     */
    public <Req, Res> Res post(String endpoint, Req body, Class<Res> responseType) {
        return sync().post(endpoint, body, responseType);
    }

    /**
     * Performs a synchronous PUT request.
     *
     * @param <Res> The response type
     * @param endpoint The API endpoint
     * @param responseType The class of the response type
     * @return The response object
     */
    public <Res> Res put(String endpoint, Class<Res> responseType) {
        return sync().put(endpoint, responseType);
    }

    /**
     * Performs a synchronous PUT request with a body.
     *
     * @param <Req> The request body type
     * @param <Res> The response type
     * @param endpoint The API endpoint
     * @param body The request body
     * @param responseType The class of the response type
     * @return The response object
     */
    public <Req, Res> Res put(String endpoint, Req body, Class<Res> responseType) {
        return sync().put(endpoint, body, responseType);
    }

    /**
     * Performs a synchronous DELETE request.
     *
     * @param <T> The response type
     * @param endpoint The API endpoint
     * @param responseType The class of the response type
     * @return The response object
     */
    public <T> T delete(String endpoint, Class<T> responseType) {
        return sync().delete(endpoint, responseType);
    }

    /**
     * Executes a synchronous request with the given request specification.
     *
     * @param <Req> The request body type
     * @param <Res> The response type
     * @param requestSpec The request specification
     * @return The response object
     */
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) {
        return sync().execute(requestSpec);
    }

    /**
     * Gets the synchronous API interface.
     * 
     * @return The synchronous API interface
     */
    public SyncApi sync() {
        return new SyncApiImpl();
    }
    
    /**
     * Gets the asynchronous API interface.
     * 
     * @return The asynchronous API interface
     */
    public AsyncApi async() {
        return new AsyncApiImpl();
    }
    
    /**
     * Gets the fluent API interface with Result pattern.
     * 
     * @return The fluent API interface
     */
    public FluentApi fluent() {
        return new FluentApiImpl();
    }

    /**
     * Configures a client option with the specified value.
     *
     * @param key   The option key from ClientOptions.
     * @param value The option value.
     * @return this client instance for method chaining
     */
    public RocketRest configure(String key, Object value) {
        options.set(key, value);

        // Apply to all clients
        syncClient.configure(key, value);
        asyncClient.configure(key, value);
        fluentClient.configure(key, value);

        // Special handling for async pool size
        if (RocketRestOptions.ASYNC_POOL_SIZE.equals(key) && value instanceof Integer) {
            logger.info("Note: Changing ASYNC_POOL_SIZE after initialization is not supported");
        }

        return this;
    }

    /**
     * Shuts down all resources used by this client.
     */
    public void shutdown() {
        asyncClient.shutdown();
        logger.info("RocketRest shutdown completed.");
    }
    
    // Helper methods for building request specs
    
    /**
     * Creates a GET request specification.
     *
     * @param <T>          The response type
     * @param endpoint     The API endpoint
     * @param responseType The class of the response type
     * @return A built request specification
     */
    private <T> RequestSpec<Void, T> createGetRequest(String endpoint, Class<T> responseType) {
        logger.debug("Creating GET request to endpoint: {}", endpoint);
        return new RequestBuilder<Void, T>()
                .endpoint(endpoint)
                .method("GET")
                .responseType(responseType)
                .build();
    }
    
    /**
     * Creates a GET request specification with query parameters.
     *
     * @param <T>          The response type
     * @param endpoint     The API endpoint
     * @param responseType The class of the response type
     * @param queryParams  The query parameters
     * @return A built request specification
     */
    private <T> RequestSpec<Void, T> createGetRequest(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
        logger.debug("Creating GET request to endpoint: {} with params: {}", endpoint, queryParams);
        return new RequestBuilder<Void, T>()
                .endpoint(endpoint)
                .method("GET")
                .queryParams(queryParams)
                .responseType(responseType)
                .build();
    }
    
    /**
     * Creates a POST request specification.
     *
     * @param <Res>        The response type
     * @param endpoint     The API endpoint
     * @param responseType The class of the response type
     * @return A built request specification
     */
    private <Res> RequestSpec<Void, Res> createPostRequest(String endpoint, Class<Res> responseType) {
        logger.debug("Creating POST request to endpoint: {}", endpoint);
        return new RequestBuilder<Void, Res>()
                .endpoint(endpoint)
                .method("POST")
                .responseType(responseType)
                .build();
    }
    
    /**
     * Creates a POST request specification with a body.
     *
     * @param <Req>        The request body type
     * @param <Res>        The response type
     * @param endpoint     The API endpoint
     * @param body         The request body
     * @param responseType The class of the response type
     * @return A built request specification
     */
    private <Req, Res> RequestSpec<Req, Res> createPostRequest(String endpoint, Req body, Class<Res> responseType) {
        logger.debug("Creating POST request to endpoint: {} with body", endpoint);
        return new RequestBuilder<Req, Res>()
                .endpoint(endpoint)
                .method("POST")
                .body(body)
                .responseType(responseType)
                .build();
    }
    
    /**
     * Creates a PUT request specification.
     *
     * @param <Res>        The response type
     * @param endpoint     The API endpoint
     * @param responseType The class of the response type
     * @return A built request specification
     */
    private <Res> RequestSpec<Void, Res> createPutRequest(String endpoint, Class<Res> responseType) {
        logger.debug("Creating PUT request to endpoint: {}", endpoint);
        return new RequestBuilder<Void, Res>()
                .endpoint(endpoint)
                .method("PUT")
                .responseType(responseType)
                .build();
    }
    
    /**
     * Creates a PUT request specification with a body.
     *
     * @param <Req>        The request body type
     * @param <Res>        The response type
     * @param endpoint     The API endpoint
     * @param body         The request body
     * @param responseType The class of the response type
     * @return A built request specification
     */
    private <Req, Res> RequestSpec<Req, Res> createPutRequest(String endpoint, Req body, Class<Res> responseType) {
        logger.debug("Creating PUT request to endpoint: {} with body", endpoint);
        return new RequestBuilder<Req, Res>()
                .endpoint(endpoint)
                .method("PUT")
                .body(body)
                .responseType(responseType)
                .build();
    }
    
    /**
     * Creates a DELETE request specification.
     *
     * @param <T>          The response type
     * @param endpoint     The API endpoint
     * @param responseType The class of the response type
     * @return A built request specification
     */
    private <T> RequestSpec<Void, T> createDeleteRequest(String endpoint, Class<T> responseType) {
        logger.debug("Creating DELETE request to endpoint: {}", endpoint);
        return new RequestBuilder<Void, T>()
                .endpoint(endpoint)
                .method("DELETE")
                .responseType(responseType)
                .build();
    }
    
    // Inner class implementations of the API interfaces
    
    /**
     * Implementation of SyncApi that delegates to the underlying DefaultApiClient.
     */
    private class SyncApiImpl implements SyncApi {
        @Override
        public <T> T get(String endpoint, Class<T> responseType) {
            return syncClient.execute(createGetRequest(endpoint, responseType));
        }
        
        @Override
        public <T> T get(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
            return syncClient.execute(createGetRequest(endpoint, responseType, queryParams));
        }
        
        @Override
        public <Res> Res post(String endpoint, Class<Res> responseType) {
            return syncClient.execute(createPostRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Res post(String endpoint, Req body, Class<Res> responseType) {
            return syncClient.execute(createPostRequest(endpoint, body, responseType));
        }
        
        @Override
        public <Res> Res put(String endpoint, Class<Res> responseType) {
            return syncClient.execute(createPutRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Res put(String endpoint, Req body, Class<Res> responseType) {
            return syncClient.execute(createPutRequest(endpoint, body, responseType));
        }
        
        @Override
        public <T> T delete(String endpoint, Class<T> responseType) {
            return syncClient.execute(createDeleteRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) {
            return syncClient.execute(requestSpec);
        }
    }
    
    /**
     * Implementation of AsyncApi that delegates to the underlying AsyncApiClient.
     */
    private class AsyncApiImpl implements AsyncApi {
        @Override
        public <T> CompletableFuture<T> get(String endpoint, Class<T> responseType) {
            return asyncClient.executeAsync(createGetRequest(endpoint, responseType));
        }
        
        @Override
        public <T> CompletableFuture<T> get(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
            return asyncClient.executeAsync(createGetRequest(endpoint, responseType, queryParams));
        }
        
        @Override
        public <Res> CompletableFuture<Res> post(String endpoint, Class<Res> responseType) {
            return asyncClient.executeAsync(createPostRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> CompletableFuture<Res> post(String endpoint, Req body, Class<Res> responseType) {
            return asyncClient.executeAsync(createPostRequest(endpoint, body, responseType));
        }
        
        @Override
        public <Res> CompletableFuture<Res> put(String endpoint, Class<Res> responseType) {
            return asyncClient.executeAsync(createPutRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> CompletableFuture<Res> put(String endpoint, Req body, Class<Res> responseType) {
            return asyncClient.executeAsync(createPutRequest(endpoint, body, responseType));
        }
        
        @Override
        public <T> CompletableFuture<T> delete(String endpoint, Class<T> responseType) {
            return asyncClient.executeAsync(createDeleteRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> CompletableFuture<Res> execute(RequestSpec<Req, Res> requestSpec) {
            return asyncClient.executeAsync(requestSpec);
        }
        
        @Override
        public void shutdown() {
            asyncClient.shutdown();
        }
    }
    
    /**
     * Implementation of FluentApi that delegates to the underlying FluentApiClient.
     */
    private class FluentApiImpl implements FluentApi {
        @Override
        public <T> Result<T, ApiError> get(String endpoint, Class<T> responseType) {
            return fluentClient.executeWithResult(createGetRequest(endpoint, responseType));
        }
        
        @Override
        public <T> Result<T, ApiError> get(String endpoint, Class<T> responseType, Map<String, String> queryParams) {
            return fluentClient.executeWithResult(createGetRequest(endpoint, responseType, queryParams));
        }
        
        @Override
        public <Res> Result<Res, ApiError> post(String endpoint, Class<Res> responseType) {
            return fluentClient.executeWithResult(createPostRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Result<Res, ApiError> post(String endpoint, Req body, Class<Res> responseType) {
            return fluentClient.executeWithResult(createPostRequest(endpoint, body, responseType));
        }
        
        @Override
        public <Res> Result<Res, ApiError> put(String endpoint, Class<Res> responseType) {
            return fluentClient.executeWithResult(createPutRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Result<Res, ApiError> put(String endpoint, Req body, Class<Res> responseType) {
            return fluentClient.executeWithResult(createPutRequest(endpoint, body, responseType));
        }
        
        @Override
        public <T> Result<T, ApiError> delete(String endpoint, Class<T> responseType) {
            return fluentClient.executeWithResult(createDeleteRequest(endpoint, responseType));
        }
        
        @Override
        public <Req, Res> Result<Res, ApiError> execute(RequestSpec<Req, Res> requestSpec) {
            return fluentClient.<Req, Res>executeWithResult(requestSpec);
        }
    }

    public void setBaseUrl(String baseUrl) {
        this.syncClient.setBaseUrl(baseUrl);
        this.asyncClient.setBaseUrl(baseUrl);
        this.fluentClient.setBaseUrl(baseUrl);
    }

    public String getAccessToken() {
        if(this.config.getAuthStrategy() instanceof AbstractOAuth2Strategy) {
            AbstractOAuth2Strategy strat = (AbstractOAuth2Strategy) this.config.getAuthStrategy();
            return strat.getAccessToken();
        }
        return null;
    }

    public Date getTokenExpiryTime() {
        if(this.config.getAuthStrategy() instanceof AbstractOAuth2Strategy) {
            AbstractOAuth2Strategy strat = (AbstractOAuth2Strategy) this.config.getAuthStrategy();
            return strat.getTokenExpiryTime();
        }
        return null;
    }
}