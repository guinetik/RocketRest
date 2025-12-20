package com.guinetik.rr.http;

/**
 * Base runtime exception for all HTTP-related errors in RocketRest.
 *
 * <p>This exception captures HTTP error details including the status code and response body,
 * making it easier to handle and diagnose API errors. As a RuntimeException, it doesn't
 * require explicit catching, but can be caught for specific error handling.
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * RuntimeException
 *   └── RocketRestException (base - catch this for all HTTP errors)
 *         ├── CircuitBreakerOpenException (circuit breaker is open)
 *         ├── TokenExpiredException (401 - token needs refresh)
 *         └── ApiException (richer error details from server)
 * </pre>
 *
 * <h2>Exception Handling</h2>
 * <pre class="language-java">{@code
 * try {
 *     User user = client.get("/users/1", User.class);
 * } catch (CircuitBreakerOpenException e) {
 *     // Service is down, fail fast
 *     System.out.println("Service unavailable, retry in " + e.getEstimatedMillisUntilReset() + "ms");
 * } catch (TokenExpiredException e) {
 *     // Token expired, re-authenticate
 *     refreshToken();
 * } catch (RocketRestException e) {
 *     // All other HTTP errors
 *     System.err.println("HTTP " + e.getStatusCode() + ": " + e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Avoiding Exceptions with Fluent API</h2>
 * <pre class="language-java">{@code
 * // Use fluent API to avoid exception handling
 * Result<User, ApiError> result = client.fluent().get("/users/1", User.class);
 *
 * result.match(
 *     user -> System.out.println("Found: " + user.getName()),
 *     error -> System.out.println("Error " + error.getStatusCode() + ": " + error.getMessage())
 * );
 * }</pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see CircuitBreakerOpenException
 * @see com.guinetik.rr.auth.TokenExpiredException
 * @see com.guinetik.rr.api.ApiException
 * @see com.guinetik.rr.result.ApiError
 * @since 1.0.0
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