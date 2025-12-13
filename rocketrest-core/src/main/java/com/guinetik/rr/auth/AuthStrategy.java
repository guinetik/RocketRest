package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;

/**
 * Interface for authentication strategies used by RocketRest.
 *
 * <p>Authentication strategies encapsulate the logic for different authentication methods,
 * from simple bearer tokens to complex OAuth 2.0 flows. Implementations are pluggable
 * and can be configured via {@link com.guinetik.rr.RocketRestConfig}.
 *
 * <h2>Available Implementations</h2>
 * <ul>
 *   <li>{@link NoAuthStrategy} - No authentication</li>
 *   <li>{@link BasicAuthStrategy} - HTTP Basic authentication</li>
 *   <li>{@link BearerTokenStrategy} - Bearer token authentication</li>
 *   <li>{@link OAuth2ClientCredentialsStrategy} - OAuth 2.0 client credentials flow</li>
 *   <li>{@link OAuth2PasswordStrategy} - OAuth 2.0 password grant flow</li>
 *   <li>{@link OAuth2AssertionStrategy} - OAuth 2.0 assertion/SAML flow</li>
 * </ul>
 *
 * <h2>Using Strategies</h2>
 * <pre class="language-java"><code>
 * // Via factory (recommended)
 * AuthStrategy bearer = AuthStrategyFactory.createBearerToken("my-token");
 * AuthStrategy basic = AuthStrategyFactory.createBasicAuth("user", "pass");
 * AuthStrategy oauth = AuthStrategyFactory.createOAuth2ClientCredentials(
 *     "client-id", "client-secret", "https://auth.example.com/token"
 * );
 *
 * // Configure in RocketRestConfig
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .authStrategy(bearer)
 *     .build();
 * </code></pre>
 *
 * <h2>Custom Strategy Implementation</h2>
 * <pre class="language-java"><code>
 * public class CustomAuthStrategy implements AuthStrategy {
 *     {@literal @}Override
 *     public AuthType getType() {
 *         return AuthType.BEARER_TOKEN;
 *     }
 *
 *     {@literal @}Override
 *     public RocketHeaders applyAuthHeaders(RocketHeaders headers) {
 *         headers.set("X-Custom-Auth", computeAuthValue());
 *         return headers;
 *     }
 *
 *     {@literal @}Override
 *     public boolean needsTokenRefresh() {
 *         return isTokenExpired();
 *     }
 *
 *     {@literal @}Override
 *     public boolean refreshCredentials() {
 *         return fetchNewToken();
 *     }
 * }
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see AuthStrategyFactory
 * @see com.guinetik.rr.RocketRestConfig
 * @since 1.0.0
 */
public interface AuthStrategy {

    /**
     * Enum representing different authentication types.
     */
    enum AuthType {
        NONE,
        BEARER_TOKEN,
        BASIC,
        OAUTH_CLIENT_CREDENTIALS,
        OAUTH_IDP,
        OAUTH_PASSWORD,
        OAUTH_ASSERTION
    }

    /**
     * Returns the auth type of this strategy.
     * @return the authentication type
     */
    AuthType getType();

    /**
     * Applies authentication headers to an existing HttpHeader object.
     * @param headers the current HttpHeader to update
     * @return the updated HttpHeader
     */
    RocketHeaders applyAuthHeaders(RocketHeaders headers);

    /**
     * Indicates whether this strategy needs a token refresh.
     * @return true if token refresh is required
     */
    boolean needsTokenRefresh();

    /**
     * Handles refreshing the authentication credentials for strategies that support it.
     * @return true if the credentials were successfully refreshed
     * @throws TokenRefreshException if the refresh operation fails
     */
    boolean refreshCredentials();
} 