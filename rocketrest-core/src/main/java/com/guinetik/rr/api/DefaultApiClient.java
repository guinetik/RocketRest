package com.guinetik.rr.api;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.http.RocketClient;
import com.guinetik.rr.http.RocketClientFactory;

/**
 * Default implementation of an API client that uses a synchronous HTTP client.
 * This class extends AbstractHttpClient and handles token refresh and SSL configuration.
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