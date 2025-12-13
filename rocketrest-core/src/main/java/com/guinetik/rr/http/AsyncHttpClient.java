package com.guinetik.rr.http;

import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.request.RequestSpec;

import javax.net.ssl.SSLContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * Asynchronous HTTP client that executes requests on a dedicated thread pool.
 *
 * <p>This client wraps any synchronous {@link RocketClient} implementation and provides
 * non-blocking request execution via {@link java.util.concurrent.CompletableFuture}.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Non-blocking request execution with CompletableFuture</li>
 *   <li>Configurable thread pool size</li>
 *   <li>Wraps any RocketClient implementation</li>
 *   <li>Proper exception propagation via CompletionException</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre class="language-java"><code>
 * ExecutorService executor = Executors.newFixedThreadPool(4);
 * AsyncHttpClient asyncClient = new AsyncHttpClient(
 *     "https://api.example.com",
 *     executor
 * );
 *
 * // Execute async request
 * CompletableFuture&lt;User&gt; future = asyncClient.executeAsync(request);
 *
 * // Handle result when ready
 * future.thenAccept(user -&gt; System.out.println("Got: " + user.getName()))
 *       .exceptionally(ex -&gt; {
 *           System.err.println("Failed: " + ex.getMessage());
 *           return null;
 *       });
 *
 * // Don't forget to shutdown
 * asyncClient.shutdown();
 * </code></pre>
 *
 * <h2>Via RocketRest</h2>
 * <pre class="language-java"><code>
 * RocketRest client = new RocketRest(config);
 *
 * client.async().get("/users/1", User.class)
 *     .thenAccept(user -&gt; System.out.println(user));
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketClient
 * @see RocketClientFactory
 * @see com.guinetik.rr.RocketRest#async()
 * @since 1.0.0
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