package com.guinetik.rr.api;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.http.RocketClient;
import com.guinetik.rr.http.RocketClientFactory;

/**
 * Default synchronous API client implementation.
 *
 * <p>This client executes HTTP requests synchronously, blocking the calling thread
 * until the response is received. It's the simplest way to interact with REST APIs
 * when you don't need asynchronous processing.
 *
 * <h2>Basic Usage</h2>
 * <pre class="language-java"><code>
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .authStrategy(AuthStrategyFactory.createBearerToken("token"))
 *     .build();
 *
 * DefaultApiClient client = new DefaultApiClient("https://api.example.com", config);
 *
 * // Execute a GET request (blocks until response)
 * RequestSpec&lt;Void, User&gt; request = RequestBuilder.get("/users/1")
 *     .responseType(User.class)
 *     .build();
 *
 * User user = client.execute(request);
 * </code></pre>
 *
 * <h2>With Custom HTTP Client</h2>
 * <pre class="language-java"><code>
 * // Create client with circuit breaker
 * RocketClient httpClient = RocketClientFactory.fromConfig(config)
 *     .withCircuitBreaker(5, 30000)
 *     .build();
 *
 * DefaultApiClient client = new DefaultApiClient(baseUrl, config, httpClient);
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see AbstractApiClient
 * @see AsyncApiClient
 * @see FluentApiClient
 * @since 1.0.0
 */
public class DefaultApiClient extends AbstractApiClient {

    /**
     * Creates a new DefaultApiClient with the specified base URL and configuration.
     *
     * @param baseUrl The base URL for API requests
     * @param config  The RocketRest configuration
     */
    public DefaultApiClient(String baseUrl, RocketRestConfig config) {
        super(baseUrl, config, createHttpClient(baseUrl, config));
    }
    
    /**
     * Creates a new DefaultApiClient with the specified base URL, configuration and a custom client.
     *
     * @param baseUrl The base URL for API requests
     * @param config  The RocketRest configuration
     * @param client  A custom RocketClient implementation
     */
    public DefaultApiClient(String baseUrl, RocketRestConfig config, RocketClient client) {
        super(baseUrl, config, client);
    }

    /**
     * Creates an HTTP client with the appropriate options.
     *
     * @param baseUrl The base URL for API requests
     * @param config  The RocketRest configuration
     * @return A new RocketClient
     */
    private static RocketClient createHttpClient(String baseUrl, RocketRestConfig config) {
        return RocketClientFactory.fromConfig(config)
                .build();
    }
} 