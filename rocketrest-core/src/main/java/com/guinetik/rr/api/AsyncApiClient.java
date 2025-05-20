package com.guinetik.rr.api;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.auth.RocketSSL;
import com.guinetik.rr.http.AsyncHttpClient;
import com.guinetik.rr.http.RocketClientFactory;
import com.guinetik.rr.request.RequestSpec;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Asynchronous API client implementation that uses an AsyncHttpClient.
 * This class extends AbstractHttpClient and handles asynchronous execution.
 */
public class AsyncApiClient extends AbstractApiClient {

    private final AsyncHttpClient asyncClient;

    /**
     * Creates a new AsyncApiClient with the specified base URL, configuration, and executor.
     *
     * @param baseUrl  The base URL for API requests
     * @param config   The RocketRest configuration
     * @param executor The ExecutorService to run requests on
     */
    public AsyncApiClient(String baseUrl, RocketRestConfig config, ExecutorService executor) {
        super(baseUrl, config, createAsyncHttpClient(baseUrl, config, executor));
        this.asyncClient = (AsyncHttpClient) httpClient;
    }
    
    /**
     * Creates a new AsyncApiClient with the specified base URL, configuration, and a
     * pre-configured AsyncHttpClient instance.
     *
     * @param baseUrl     The base URL for API requests
     * @param config      The RocketRest configuration
     * @param asyncClient A pre-configured AsyncHttpClient instance
     */
    public AsyncApiClient(String baseUrl, RocketRestConfig config, AsyncHttpClient asyncClient) {
        super(baseUrl, config, asyncClient);
        this.asyncClient = asyncClient;
    }

    /**
     * Executes a request asynchronously.
     *
     * @param <Req>       The type of the request
     * @param <Res>       The type of the response
     * @param requestSpec The request specification
     * @return A CompletableFuture that will complete with the response
     */
    public <Req, Res> CompletableFuture<Res> executeAsync(RequestSpec<Req, Res> requestSpec) {
        return asyncClient.executeAsync(requestSpec);
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        asyncClient.shutdown();
    }

    /**
     * Creates an AsyncHttpClient with the appropriate options.
     *
     * @param baseUrl  The base URL for API requests
     * @param config   The RocketRest configuration
     * @param executor The ExecutorService to run requests on
     * @return A new AsyncHttpClient
     */
    private static AsyncHttpClient createAsyncHttpClient(String baseUrl, RocketRestConfig config, ExecutorService executor) {
        return RocketClientFactory.fromConfig(config)
                .withExecutorService(executor)
                .buildAsync();
    }
} 