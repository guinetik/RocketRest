package com.guinetik.rr.http;

import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.request.RequestSpec;

import javax.net.ssl.SSLContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * Asynchronous HTTP client that wraps a synchronous HttpRequestClient
 * implementation and executes requests on a separate thread pool.
 */
public class AsyncHttpClient implements RocketClient {

    private final RocketClient delegate;
    private final ExecutorService executor;

    /**
     * Creates a new AsyncHttpClient with the specified delegate client and executor.
     *
     * @param delegate The underlying HTTP client to delegate requests to
     * @param executor The executor service to run requests on
     */
    public AsyncHttpClient(RocketClient delegate, ExecutorService executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    /**
     * Creates a new AsyncHttpClient with a DefaultHttpClient as the delegate.
     *
     * @param baseUrl  The base URL for API requests
     * @param executor The executor service to run requests on
     */
    public AsyncHttpClient(String baseUrl, ExecutorService executor) {
        this(new DefaultHttpClient(baseUrl), executor);
    }

    /**
     * Creates a new AsyncHttpClient with a DefaultHttpClient as the delegate and client options.
     *
     * @param baseUrl       The base URL for API requests
     * @param clientOptions The client options
     * @param executor      The executor service to run requests on
     */
    public AsyncHttpClient(String baseUrl, RocketRestOptions clientOptions, ExecutorService executor) {
        this(new DefaultHttpClient(baseUrl, clientOptions), executor);
    }

    @Override
    public void configureSsl(SSLContext sslContext) {
        delegate.configureSsl(sslContext);
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.delegate.setBaseUrl(baseUrl);
    }

    @Override
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
        // This method is generally not used directly with AsyncHttpClient,
        // but it's implemented for HTTP client interface compatibility
        return delegate.execute(requestSpec);
    }

    /**
     * Executes an HTTP request asynchronously.
     *
     * @param <Req>       The type of the request body
     * @param <Res>       The type of the response
     * @param requestSpec The request specification
     * @return A CompletableFuture that will complete with the response
     */
    public <Req, Res> CompletableFuture<Res> executeAsync(RequestSpec<Req, Res> requestSpec) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.execute(requestSpec);
            } catch (RocketRestException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executor.shutdown();
    }
} 