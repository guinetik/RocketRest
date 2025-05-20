package com.guinetik.rr.auth;

/**
 * Exception thrown when token refresh fails.
 */
public class TokenRefreshException extends RuntimeException {
    
    public TokenRefreshException(String message) {
        super(message);
    }

    public TokenRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
} 