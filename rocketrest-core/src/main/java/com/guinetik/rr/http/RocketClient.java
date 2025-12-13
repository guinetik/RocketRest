package com.guinetik.rr.http;

import com.guinetik.rr.request.RequestSpec;

/**
 * Core interface for HTTP request execution in RocketRest.
 *
 * <p>This abstraction allows different HTTP client implementations while maintaining
 * a consistent API. The default implementation uses Java's {@code HttpURLConnection},
 * but custom implementations can be provided.
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link DefaultHttpClient} - Synchronous client using HttpURLConnection</li>
 *   <li>{@link AsyncHttpClient} - Wraps any RocketClient for async execution</li>
 *   <li>{@link FluentHttpClient} - Returns {@link com.guinetik.rr.result.Result} instead of exceptions</li>
 *   <li>{@link CircuitBreakerClient} - Decorator adding circuit breaker resilience</li>
 *   <li>{@link MockRocketClient} - For testing without real HTTP calls</li>
 * </ul>
 *
 * <h2>Creating Clients</h2>
 * <pre class="language-java"><code>
 * // Via factory (recommended)
 * RocketClient client = RocketClientFactory.builder("https://api.example.com")
 *     .withOptions(options)
 *     .withCircuitBreaker(5, 30000)
 *     .build();
 *
 * // Execute request
 * RequestSpec&lt;Void, User&gt; request = RequestBuilder.get("/users/1")
 *     .responseType(User.class)
 *     .build();
 *
 * User user = client.execute(request);
 * </code></pre>
 *
 * <h2>Custom Implementation</h2>
 * <pre class="language-java"><code>
 * public class OkHttpRocketClient implements RocketClient {
 *     private final OkHttpClient okHttp = new OkHttpClient();
 *
 *     {@literal @}Override
 *     public &lt;Req, Res&gt; Res execute(RequestSpec&lt;Req, Res&gt; spec) {
 *         // Implement using OkHttp
 *     }
 *     // ... other methods
 * }
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketClientFactory
 * @see DefaultHttpClient
 * @since 1.0.0
 */
public interface RocketClient {
    
    /**
     * Executes an HTTP request based on the provided request specification.
     *
     * @param <Req>       The type of the request body.
     * @param <Res>       The type of the response.
     * @param requestSpec The specification of the request to be executed.
     * @return The response object.
     * @throws RocketRestException If an error occurs during the request execution.
     */
    <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException;
    
    /**
     * Sets the SSL context to be used for HTTPS requests.
     * 
     * @param sslContext The SSL context to use.
     */
    void configureSsl(javax.net.ssl.SSLContext sslContext);

    void setBaseUrl(String baseUrl);
} 