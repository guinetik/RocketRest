package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;
import java.util.function.BooleanSupplier;

/**
 * Authentication strategy that uses Bearer token authentication.
 *
 * <p>This strategy adds an {@code Authorization: Bearer <token>} header to all requests.
 * It's suitable for APIs that use API keys, JWT tokens, or other bearer-style authentication.
 *
 * <h2>Basic Usage</h2>
 * <pre class="language-java"><code>
 * // Create via factory (recommended)
 * AuthStrategy auth = AuthStrategyFactory.createBearerToken("my-api-token");
 *
 * // Configure client
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .authStrategy(auth)
 *     .build();
 * </code></pre>
 *
 * <h2>With Custom Refresh Logic</h2>
 * <p>For tokens that expire, you can provide custom refresh logic:
 * <pre class="language-java"><code>
 * AtomicReference&lt;String&gt; tokenRef = new AtomicReference&lt;&gt;("initial-token");
 *
 * AuthStrategy auth = AuthStrategyFactory.createBearerToken(tokenRef.get(), () -&gt; {
 *     try {
 *         String newToken = myAuthService.refreshToken();
 *         tokenRef.set(newToken);
 *         return true;
 *     } catch (Exception e) {
 *         return false;
 *     }
 * });
 * </code></pre>
 *
 * <h2>For OAuth Tokens</h2>
 * <p>If your bearer token comes from an OAuth flow, consider using the dedicated OAuth strategies
 * which handle token refresh automatically:
 * <ul>
 *   <li>{@link OAuth2ClientCredentialsStrategy}</li>
 *   <li>{@link OAuth2PasswordStrategy}</li>
 *   <li>{@link OAuth2AssertionStrategy}</li>
 * </ul>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see AuthStrategy
 * @see AuthStrategyFactory#createBearerToken(String)
 * @since 1.0.0
 */
public class BearerTokenStrategy implements AuthStrategy {

    private final String token;
    private final BooleanSupplier refreshTokenLogic;

    /**
     * Creates a new BearerTokenStrategy with no custom refresh logic.
     * In this case, {@link #refreshCredentials()} will always return {@code false}.
     *
     * @param token the bearer token
     */
    public BearerTokenStrategy(String token) {
        this(token, () -> false);
    }

    /**
     * Creates a new BearerTokenStrategy with custom token refresh logic.
     *
     * @param token             the bearer token
     * @param refreshTokenLogic a {@link BooleanSupplier} that will be invoked by {@link #refreshCredentials()}.
     *                          It should return {@code true} if the token was successfully refreshed, {@code false} otherwise.
     */
    public BearerTokenStrategy(String token, BooleanSupplier refreshTokenLogic) {
        this.token = token;
        this.refreshTokenLogic = refreshTokenLogic != null ? refreshTokenLogic : () -> false;
    }

    @Override
    public AuthType getType() {
        return AuthType.BEARER_TOKEN;
    }

    @Override
    public RocketHeaders applyAuthHeaders(RocketHeaders headers) {
        if (token != null && !token.isEmpty()) {
            headers.bearerAuth(token);
        }
        return headers;
    }

    @Override
    public boolean needsTokenRefresh() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If a custom {@code refreshTokenLogic} was provided during construction,
     * this method will invoke it and return its result.
     * <p>
     * If no custom logic was provided, this method always returns {@code false},
     * as this strategy itself does not handle credential refresh.
     */
    @Override
    public boolean refreshCredentials() {
        return this.refreshTokenLogic.getAsBoolean();
    }
} 