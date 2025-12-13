package com.guinetik.rr.http;

/**
 * Runtime exception thrown when HTTP request execution fails in RocketRest.
 *
 * <p>This exception captures HTTP error details including the status code and response body,
 * making it easier to handle and diagnose API errors. As a RuntimeException, it doesn't
 * require explicit catching, but can be caught for specific error handling.
 *
 * <h2>Exception Handling</h2>
 * <pre class="language-java"><code>
 * try {
 *     User user = client.get("/users/1", User.class);
 * } catch (RocketRestException e) {
 *     int status = e.getStatusCode();
 *     String body = e.getResponseBody();
 *
 *     if (status == 404) {
 *         System.out.println("User not found");
 *     } else if (status == 401) {
 *         System.out.println("Authentication required");
 *     } else {
 *         System.err.println("Request failed: " + e.getMessage());
 *         System.err.println("Response: " + body);
 *     }
 * }
 * </code></pre>
 *
 * <h2>Avoiding Exceptions with Fluent API</h2>
 * <pre class="language-java"><code>
 * // Use fluent API to avoid exception handling
 * Result&lt;User, ApiError&gt; result = client.fluent().get("/users/1", User.class);
 *
 * result.match(
 *     user -&gt; System.out.println("Found: " + user.getName()),
 *     error -&gt; System.out.println("Error " + error.getStatusCode() + ": " + error.getMessage())
 * );
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see CircuitBreakerOpenException
 * @see com.guinetik.rr.auth.TokenExpiredException
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