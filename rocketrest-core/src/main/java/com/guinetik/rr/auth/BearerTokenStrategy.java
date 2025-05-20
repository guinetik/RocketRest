package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;
import java.util.function.BooleanSupplier;

/**
 * Authentication strategy that uses Bearer token for authentication.
 * This strategy simply adds a provided Bearer token to the Authorization header.
 * It can optionally delegate token refresh logic to a provided {@link BooleanSupplier}.
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