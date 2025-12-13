package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;

/**
 * Authentication strategy that uses HTTP Basic authentication.
 *
 * <p>This strategy adds an {@code Authorization: Basic <base64>} header to all requests,
 * where the base64 value is the encoded {@code username:password} string.
 *
 * <h2>Usage</h2>
 * <pre class="language-java"><code>
 * // Create via factory (recommended)
 * AuthStrategy auth = AuthStrategyFactory.createBasicAuth("username", "password");
 *
 * // Configure client
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .authStrategy(auth)
 *     .build();
 *
 * RocketRest client = new RocketRest(config);
 * </code></pre>
 *
 * <p><b>Security Note:</b> Basic authentication transmits credentials in base64 encoding
 * (not encryption). Always use HTTPS when using basic authentication.
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see AuthStrategy
 * @see AuthStrategyFactory#createBasicAuth(String, String)
 * @since 1.0.0
 */
public class BasicAuthStrategy implements AuthStrategy {

    private final String username;
    private final String password;

    /**
     * Creates a new BasicAuthStrategy.
     *
     * @param username the username for basic authentication
     * @param password the password for basic authentication
     */
    public BasicAuthStrategy(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public AuthType getType() {
        return AuthType.BASIC;
    }

    @Override
    public RocketHeaders applyAuthHeaders(RocketHeaders headers) {
        if (username != null && password != null) {
            headers.basicAuth(username, password);
        }
        return headers;
    }

    @Override
    public boolean needsTokenRefresh() {
        return false;
    }

    @Override
    public boolean refreshCredentials() {
        // Basic auth doesn't require credential refresh
        return true;
    }
} 