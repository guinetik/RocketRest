package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;

/**
 * Interface for authentication strategies used by RocketRest.
 * Different implementations provide various authentication methods.
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