package com.guinetik.rr.api;

import com.guinetik.rr.http.RocketRestException;

/**
 * Exception thrown when an API request fails with rich error details.
 *
 * <p>This exception extends {@link RocketRestException} and provides additional
 * context about API failures including the error message from the server.
 * Use this when you need more details than the base exception provides.
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * RuntimeException
 *   └── RocketRestException (base HTTP exception)
 *         ├── CircuitBreakerOpenException
 *         ├── TokenExpiredException
 *         └── ApiException (richer error details)
 *               └── errorMessage: Server error message
 * </pre>
 *
 * <h2>Handling ApiException</h2>
 * <pre class="language-java">{@code
 * try {
 *     User user = client.get("/users/999", User.class);
 * } catch (ApiException e) {
 *     System.err.println("Status: " + e.getStatusCode());
 *     System.err.println("Message: " + e.getErrorMessage());
 *     System.err.println("Body: " + e.getResponseBody());
 * } catch (RocketRestException e) {
 *     // Handles all other HTTP exceptions
 *     System.err.println("HTTP error: " + e.getStatusCode());
 * }
 * }</pre>
 *
 * <h2>Avoiding Exceptions with Result Pattern</h2>
 * <p>Consider using the fluent API with {@link com.guinetik.rr.result.Result} to avoid exceptions:
 * <pre class="language-java">{@code
 * Result<User, ApiError> result = client.fluent().get("/users/999", User.class);
 * result.match(
 *     user -> handleSuccess(user),
 *     error -> handleError(error)  // No exception thrown
 * );
 * }</pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketRestException
 * @see com.guinetik.rr.result.Result
 * @see com.guinetik.rr.result.ApiError
 * @since 1.0.0
 */
public class ApiException extends RocketRestException {

    private static final long serialVersionUID = 1L;

    private final String errorMessage;

    /**
     * Creates a new ApiException with full error details.
     *
     * @param message The exception message
     * @param responseBody The raw response body from the server
     * @param errorMessage The parsed error message from the server
     * @param statusCode The HTTP status code
     */
    public ApiException(String message, String responseBody, String errorMessage, int statusCode) {
        super(message, statusCode, responseBody);
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a new ApiException with just a message.
     *
     * @param message The exception message
     */
    public ApiException(String message) {
        super(message);
        this.errorMessage = null;
    }

    /**
     * Creates a new ApiException with a message and cause.
     *
     * @param message The exception message
     * @param cause The underlying cause
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorMessage = null;
    }

    /**
     * Gets the parsed error message from the server response.
     * This is often a more user-friendly message extracted from the response body.
     *
     * @return The server error message, or null if not available
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
