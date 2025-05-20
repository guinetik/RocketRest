package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;

/**
 * Authentication strategy that performs no authentication.
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