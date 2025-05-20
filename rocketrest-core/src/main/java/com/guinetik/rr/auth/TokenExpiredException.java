package com.guinetik.rr.auth;

/**
 * Exception thrown when an API request fails due to token expiration.
 */
public class TokenExpiredException extends RuntimeException {
    
    public TokenExpiredException(String message) {
        super(message);
    }
    
    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
} 