package com.guinetik.rr;

import com.guinetik.rr.auth.AuthStrategy;
import com.guinetik.rr.auth.AuthStrategyFactory;

import java.util.function.Consumer;

/**
 * Configuration container for {@link RocketRest} clients.
 *
 * <p>This class holds all configuration options needed to create a RocketRest client,
 * including the service URL, authentication strategy, and default request options.
 * Use the fluent {@link Builder} to construct instances.
 *
 * <h2>Basic Usage</h2>
 * <pre class="language-java"><code>
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .build();
 *
 * RocketRest client = new RocketRest(config);
 * </code></pre>
 *
 * <h2>With Authentication</h2>
 * <pre class="language-java"><code>
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .authStrategy(AuthStrategyFactory.createBearerToken("my-api-token"))
 *     .build();
 * </code></pre>
 *
 * <h2>With Default Options</h2>
 * <pre class="language-java"><code>
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .authStrategy(AuthStrategyFactory.createBasicAuth("user", "pass"))
 *     .defaultOptions(options -&gt; {
 *         options.set(RocketRestOptions.RETRY_ENABLED, true);
 *         options.set(RocketRestOptions.MAX_RETRIES, 3);
 *         options.set(RocketRestOptions.CONNECT_TIMEOUT, 5000);
 *         options.set(RocketRestOptions.LOG_REQUEST_BODY, true);
 *     })
 *     .build();
 * </code></pre>
 *
 * <h2>OAuth2 Client Credentials</h2>
 * <pre class="language-java"><code>
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .tokenUrl("https://auth.example.com/oauth/token")
 *     .authStrategy(AuthStrategyFactory.createOAuth2ClientCredentials(
 *         "https://auth.example.com/oauth/token",
 *         "client-id",
 *         "client-secret"
 *     ))
 *     .build();
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketRest
 * @see RocketRestOptions
 * @see com.guinetik.rr.auth.AuthStrategy
 * @since 1.0.0
 */
public class RocketRestConfig {

    /**
     * Builder for creating RocketRestConfig instances.
     */
    public static class Builder {
        private final String serviceUrl;
        private final RocketRestOptions defaultOptions = new RocketRestOptions();
        private String tokenUrl;
        private AuthStrategy authStrategy = AuthStrategyFactory.createNoAuth();

        public Builder(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        /**
         * Sets the token URL for OAuth flows.
         *
         * @param tokenUrl the token URL
         * @return this builder instance
         */
        public Builder tokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
            return this;
        }

        /**
         * Sets the authentication strategy.
         *
         * @param authStrategy the authentication strategy
         * @return this builder instance
         */
        public Builder authStrategy(AuthStrategy authStrategy) {
            this.authStrategy = authStrategy;
            return this;
        }


        /**
         * Sets default client options that will be used by clients created with this config.
         *
         * @param optionsConfigurer a consumer that configures the default options
         * @return this builder instance
         */
        public Builder defaultOptions(Consumer<RocketRestOptions> optionsConfigurer) {
            optionsConfigurer.accept(this.defaultOptions);
            return this;
        }

        /**
         * Sets a specific default client option.
         *
         * @param key   the option key
         * @param value the option value
         * @return this builder instance
         */
        public Builder defaultOption(String key, Object value) {
            this.defaultOptions.set(key, value);
            return this;
        }

        /**
         * Builds a new RocketRestConfig instance.
         *
         * @return a new RocketRestConfig instance
         */
        public RocketRestConfig build() {
            return new RocketRestConfig(this);
        }
    }
    private String serviceUrl;
    private final String tokenUrl;
    private final AuthStrategy authStrategy;
    // Default client options
    private final RocketRestOptions defaultOptions;

    protected RocketRestConfig(Builder builder) {
        this.serviceUrl = builder.serviceUrl;
        this.tokenUrl = builder.tokenUrl;
        this.authStrategy = builder.authStrategy != null ? builder.authStrategy : AuthStrategyFactory.createNoAuth();
        this.defaultOptions = builder.defaultOptions;
    }

    /**
     * Gets the service URL.
     *
     * @return the service URL
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Gets the token URL for OAuth flows.
     *
     * @return the token URL
     */
    public String getTokenUrl() {
        return tokenUrl;
    }

    /**
     * Gets the authentication strategy.
     *
     * @return the authentication strategy
     */
    public AuthStrategy getAuthStrategy() {
        return authStrategy;
    }

    /**
     * Gets the default client options for clients created with this config.
     *
     * @return the default client options
     */
    public RocketRestOptions getDefaultOptions() {
        return defaultOptions;
    }

    public void setServiceUrl(String baseUrl) {
        this.serviceUrl = baseUrl;
    }

    /**
     * Creates a new builder for RocketRestConfig.
     *
     * @param serviceUrl the base URL for the API service
     * @return a new builder instance
     */
    public static Builder builder(String serviceUrl) {
        return new Builder(serviceUrl);
    }
} 