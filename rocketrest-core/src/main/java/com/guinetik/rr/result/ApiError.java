package com.guinetik.rr.result;

/**
 * Represents an API error in the Result pattern.
 * This class encapsulates information about API errors without using exceptions.
 */
public class ApiError {
    private final String message;
    private final int statusCode;
    private final String responseBody;
    private final ErrorType errorType;

    /**
     * Enum representing different types of errors that can occur.
     */
    public enum ErrorType {
        /** HTTP error from server */
        HTTP_ERROR,
        
        /** Network connectivity error */
        NETWORK_ERROR,
        
        /** Error parsing response */
        PARSE_ERROR,
        
        /** Authentication/authorization error */
        AUTH_ERROR,
        
        /** Client configuration error */
        CONFIG_ERROR,
        
        /** Circuit breaker is open */
        CIRCUIT_OPEN,
        
        /** Unknown error */
        UNKNOWN
    }

    /**
     * Constructs a new ApiError with the specified parameters.
     * 
     * @param message Error message
     * @param statusCode HTTP status code (may be 0 for non-HTTP errors)
     * @param responseBody Response body or null if not available
     * @param errorType Type of error that occurred
     */
    public ApiError(String message, int statusCode, String responseBody, ErrorType errorType) {
        this.message = message;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.errorType = errorType;
    }

    /**
     * Constructs an HTTP error with status code.
     * 
     * @param message Error message
     * @param statusCode HTTP status code
     * @param responseBody Response body
     * @return A new ApiError representing an HTTP error
     */
    public static ApiError httpError(String message, int statusCode, String responseBody) {
        return new ApiError(message, statusCode, responseBody, ErrorType.HTTP_ERROR);
    }

    /**
     * Constructs a network error.
     * 
     * @param message Error message
     * @return A new ApiError representing a network error
     */
    public static ApiError networkError(String message) {
        return new ApiError(message, 0, null, ErrorType.NETWORK_ERROR);
    }

    /**
     * Constructs a parse error.
     * 
     * @param message Error message
     * @param responseBody The response that couldn't be parsed
     * @return A new ApiError representing a parse error
     */
    public static ApiError parseError(String message, String responseBody) {
        return new ApiError(message, 0, responseBody, ErrorType.PARSE_ERROR);
    }

    /**
     * Constructs an authentication error.
     * 
     * @param message Error message
     * @param statusCode HTTP status code (typically 401)
     * @param responseBody Response body
     * @return A new ApiError representing an authentication error
     */
    public static ApiError authError(String message, int statusCode, String responseBody) {
        return new ApiError(message, statusCode, responseBody, ErrorType.AUTH_ERROR);
    }

    /**
     * Constructs a configuration error.
     * 
     * @param message Error message
     * @return A new ApiError representing a configuration error
     */
    public static ApiError configError(String message) {
        return new ApiError(message, 0, null, ErrorType.CONFIG_ERROR);
    }

    /**
     * Constructs a circuit breaker open error.
     * 
     * @param message Error message
     * @return A new ApiError representing a circuit breaker open error
     */
    public static ApiError circuitOpenError(String message) {
        return new ApiError(message, 0, null, ErrorType.CIRCUIT_OPEN);
    }

    /**
     * Gets the error message.
     * 
     * @return The error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the HTTP status code.
     * 
     * @return The HTTP status code, or 0 if not applicable
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the response body.
     * 
     * @return The response body, or null if not available
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Gets the error type.
     * 
     * @return The error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Checks if this error is of the specified type.
     * 
     * @param type The error type to check
     * @return true if this error is of the specified type, false otherwise
     */
    public boolean isType(ErrorType type) {
        return this.errorType == type;
    }

    /**
     * Checks if this error is an HTTP error with the specified status code.
     * 
     * @param statusCode The HTTP status code to check
     * @return true if this error is an HTTP error with the specified status code
     */
    public boolean hasStatusCode(int statusCode) {
        return this.statusCode == statusCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(errorType).append(": ").append(message);
        
        if (statusCode > 0) {
            sb.append(" (Status: ").append(statusCode).append(")");
        }
        
        return sb.toString();
    }
} 