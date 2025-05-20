package com.guinetik.rr.http;

/**
 * Exception thrown by RocketRest when HTTP request execution fails.
 */
public class RocketRestException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    private int statusCode;
    private String responseBody;
    
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message
     */
    public RocketRestException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause The cause
     */
    public RocketRestException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new exception with the specified detail message, status code, and response body.
     *
     * @param message The detail message
     * @param statusCode The HTTP status code
     * @param responseBody The response body
     */
    public RocketRestException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    /**
     * Gets the HTTP status code.
     *
     * @return The HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Gets the response body.
     *
     * @return The response body
     */
    public String getResponseBody() {
        return responseBody;
    }
} 