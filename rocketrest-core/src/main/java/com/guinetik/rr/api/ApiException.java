package com.guinetik.rr.api;

/**
 * General exception class for API errors.
 */
public class ApiException extends RuntimeException {

    private final String responseBody;
    private final String errorMessage;
    private final int statusCode;

    public ApiException(String message, String responseBody, String errorMessage, int statusCode) {
        super(message);
        this.responseBody = responseBody;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    public ApiException(String message) {
        super(message);
        responseBody = null;
        errorMessage = null;
        statusCode = -1;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        responseBody = null;
        errorMessage = null;
        statusCode = -1;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
