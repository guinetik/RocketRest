package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;

/**
 * Authentication strategy that performs no authentication.
 *
 * <p>This is the default strategy used when no authentication is configured.
 * It does not add any authentication headers to requests.
 *
 * <h2>Usage</h2>
 * <pre class="language-java"><code>
 * // Explicitly create no-auth strategy
 * AuthStrategy auth = AuthStrategyFactory.createNoAuth();
 *
 * // Or simply don't set an auth strategy (default behavior)
 * RocketRestConfig config = RocketRestConfig.builder("https://public-api.example.com")
 *     .build();  // Uses NoAuthStrategy by default
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see AuthStrategy
 * @see AuthStrategyFactory#createNoAuth()
 * @since 1.0.0
 */
public class NoAuthStrategy implements AuthStrategy {

    @Override
    public AuthType getType() {
        return AuthType.NONE;
    }

    @Override
    public RocketHeaders applyAuthHeaders(RocketHeaders headers) {
        // No headers to apply
        return headers;
    }

    @Override
    public boolean needsTokenRefresh() {
        return false;
    }

    @Override
    public boolean refreshCredentials() {
        // Nothing to refresh
        return true;
    }
} 