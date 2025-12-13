package com.guinetik.rr.api;

/**
 * Runtime exception thrown when an API request fails.
 *
 * <p>This exception wraps HTTP errors, network failures, and other API-related issues.
 * It provides access to the HTTP status code, error message, and raw response body
 * when available.
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * RuntimeException
 *   └── ApiException (general API failures)
 *         ├── statusCode: HTTP status code (e.g., 404, 500)
 *         ├── errorMessage: Server error message
 *         └── responseBody: Raw response content
 * </pre>
 *
 * <h2>Handling ApiException</h2>
 * <pre class="language-java"><code>
 * try {
 *     User user = client.get("/users/999", User.class);
 * } catch (ApiException e) {
 *     System.err.println("Status: " + e.getStatusCode());
 *     System.err.println("Message: " + e.getErrorMessage());
 *     System.err.println("Body: " + e.getResponseBody());
 * }
 * </code></pre>
 *
 * <h2>Avoiding Exceptions with Result Pattern</h2>
 * <p>Consider using the fluent API with {@link com.guinetik.rr.result.Result} to avoid exceptions:
 * <pre class="language-java"><code>
 * Result&lt;User, ApiError&gt; result = client.fluent().get("/users/999", User.class);
 * result.match(
 *     user -&gt; handleSuccess(user),
 *     error -&gt; handleError(error)  // No exception thrown
 * );
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see com.guinetik.rr.result.Result
 * @see com.guinetik.rr.result.ApiError
 * @since 1.0.0
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
