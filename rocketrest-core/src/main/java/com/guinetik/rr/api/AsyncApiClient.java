package com.guinetik.rr.api;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.http.AsyncHttpClient;
import com.guinetik.rr.http.RocketClientFactory;
import com.guinetik.rr.request.RequestSpec;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Asynchronous API client that returns {@link CompletableFuture} for non-blocking operations.
 *
 * <p>This client executes HTTP requests asynchronously using a configurable thread pool,
 * allowing the calling thread to continue processing while waiting for responses.
 * Ideal for high-throughput applications or when making multiple concurrent API calls.
 *
 * <h2>Basic Usage</h2>
 * <pre class="language-java"><code>
 * ExecutorService executor = Executors.newFixedThreadPool(4);
 * AsyncApiClient client = new AsyncApiClient("https://api.example.com", config, executor);
 *
 * // Execute async request
 * CompletableFuture&lt;User&gt; future = client.executeAsync(
 *     RequestBuilder.get("/users/1")
 *         .responseType(User.class)
 *         .build()
 * );
 *
 * // Process result when ready
 * future.thenAccept(user -&gt; System.out.println("Got: " + user.getName()));
 *
 * // Don't forget to shutdown
 * client.shutdown();
 * </code></pre>
 *
 * <h2>Multiple Concurrent Requests</h2>
 * <pre class="language-java"><code>
 * CompletableFuture&lt;User&gt; user1 = client.executeAsync(getRequest("/users/1"));
 * CompletableFuture&lt;User&gt; user2 = client.executeAsync(getRequest("/users/2"));
 * CompletableFuture&lt;User&gt; user3 = client.executeAsync(getRequest("/users/3"));
 *
 * // Wait for all to complete
 * CompletableFuture.allOf(user1, user2, user3)
 *     .thenRun(() -&gt; System.out.println("All users loaded"));
 * </code></pre>
 *
 * <h2>Error Handling</h2>
 * <pre class="language-java"><code>
 * client.executeAsync(request)
 *     .thenAccept(user -&gt; processUser(user))
 *     .exceptionally(ex -&gt; {
 *         System.err.println("Failed: " + ex.getMessage());
 *         return null;
 *     });
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see AbstractApiClient
 * @see DefaultApiClient
 * @see FluentApiClient
 * @since 1.0.0
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