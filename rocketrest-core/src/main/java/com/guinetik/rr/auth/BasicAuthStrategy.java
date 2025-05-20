package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;

/**
 * Authentication strategy that uses HTTP Basic authentication.
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