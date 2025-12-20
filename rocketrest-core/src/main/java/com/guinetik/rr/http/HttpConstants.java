package com.guinetik.rr.http;

/**
 * Centralized constants for HTTP operations used throughout RocketRest.
 *
 * <p>This utility class contains all HTTP-related constants including methods, status codes,
 * timeouts, headers, and circuit breaker configuration. Using these constants ensures
 * consistency and maintainability across the codebase.
 *
 * <h2>HTTP Methods</h2>
 * <pre class="language-java"><code>
 * // Use HTTP method constants
 * String method = HttpConstants.Methods.GET;
 * String postMethod = HttpConstants.Methods.POST;
 *
 * // In request building
 * RequestSpec request = new RequestBuilder()
 *     .method(HttpConstants.Methods.POST)
 *     .endpoint("/users")
 *     .build();
 * </code></pre>
 *
 * <h2>Status Code Handling</h2>
 * <pre class="language-java"><code>
 * // Check response status
 * if (statusCode == HttpConstants.StatusCodes.OK) {
 *     // Handle success
 * } else if (statusCode == HttpConstants.StatusCodes.UNAUTHORIZED) {
 *     // Handle auth failure
 * }
 *
 * // Check ranges
 * if (statusCode &gt;= HttpConstants.StatusCodes.SUCCESS_MIN &amp;&amp;
 *     statusCode &lt;= HttpConstants.StatusCodes.SUCCESS_MAX) {
 *     // 2xx response
 * }
 * </code></pre>
 *
 * <h2>Circuit Breaker Configuration</h2>
 * <pre class="language-java"><code>
 * RocketRestOptions options = new RocketRestOptions();
 * options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_ENABLED, true);
 * options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_THRESHOLD, 5);
 * options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_RESET_TIMEOUT_MS, 30000);
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @since 1.0.0
 */
public final class HttpConstants {

    /**
     * HTTP methods
     */
    public static final class Methods {
        /** HTTP GET method */
        public static final String GET = "GET";
        /** HTTP POST method */
        public static final String POST = "POST";
        /** HTTP PUT method */
        public static final String PUT = "PUT";
        /** HTTP PATCH method */
        public static final String PATCH = "PATCH";
        /** HTTP DELETE method */
        public static final String DELETE = "DELETE";
        /** HTTP HEAD method */
        public static final String HEAD = "HEAD";
        /** HTTP OPTIONS method */
        public static final String OPTIONS = "OPTIONS";
    }

    /**
     * HTTP status codes
     */
    public static final class StatusCodes {
        // 2xx Success
        /** Status code (200): Request has succeeded */
        public static final int OK = 200;
        /** Status code (201): Request has succeeded and new resource has been created */
        public static final int CREATED = 201;
        /** Status code (202): Request has been accepted for processing */
        public static final int ACCEPTED = 202;
        /** Status code (204): Server fulfilled request but does not need to return content */
        public static final int NO_CONTENT = 204;

        // 3xx Redirection
        /** Status code (301): Requested resource has been permanently moved */
        public static final int MOVED_PERMANENTLY = 301;
        /** Status code (302): Requested resource has been temporarily moved */
        public static final int FOUND = 302;
        /** Status code (303): Response to request can be found under different URI */
        public static final int SEE_OTHER = 303;
        /** Status code (304): Resource has not been modified since last request */
        public static final int NOT_MODIFIED = 304;

        // 4xx Client Errors
        /** Status code (400): Server cannot process request due to client error */
        public static final int BAD_REQUEST = 400;
        /** Status code (401): Authentication is required and has failed or not been provided */
        public static final int UNAUTHORIZED = 401;
        /** Status code (403): Server understood request but refuses to authorize it */
        public static final int FORBIDDEN = 403;
        /** Status code (404): Server cannot find requested resource */
        public static final int NOT_FOUND = 404;
        /** Status code (405): Request method is not supported for the requested resource */
        public static final int METHOD_NOT_ALLOWED = 405;
        /** Status code (409): Request conflicts with current state of the server */
        public static final int CONFLICT = 409;
        /** Status code (410): Requested resource is no longer available */
        public static final int GONE = 410;

        // 5xx Server Errors
        /** Status code (500): Server encountered an unexpected condition */
        public static final int INTERNAL_SERVER_ERROR = 500;
        /** Status code (501): Server does not support the functionality required */
        public static final int NOT_IMPLEMENTED = 501;
        /** Status code (502): Server received an invalid response from an upstream server */
        public static final int BAD_GATEWAY = 502;
        /** Status code (503): Server is currently unavailable */
        public static final int SERVICE_UNAVAILABLE = 503;

        // Status code ranges
        /** Minimum status code for success responses (200-299) */
        public static final int SUCCESS_MIN = 200;
        /** Maximum status code for success responses (200-299) */
        public static final int SUCCESS_MAX = 299;
        /** Minimum status code for redirection responses (300-399) */
        public static final int REDIRECT_MIN = 300;
        /** Maximum status code for redirection responses (300-399) */
        public static final int REDIRECT_MAX = 399;
        /** Minimum status code for client error responses (400-499) */
        public static final int CLIENT_ERROR_MIN = 400;
        /** Maximum status code for client error responses (400-499) */
        public static final int CLIENT_ERROR_MAX = 499;
        /** Minimum status code for server error responses (500-599) */
        public static final int SERVER_ERROR_MIN = 500;
        /** Maximum status code for server error responses (500-599) */
        public static final int SERVER_ERROR_MAX = 599;
    }

    /**
     * Connection timeouts in milliseconds
     */
    public static final class Timeouts {
        /** Default connection timeout in milliseconds (10 seconds) */
        public static final int DEFAULT_CONNECT_TIMEOUT = 10000; // 10 seconds
        /** Default read timeout in milliseconds (30 seconds) */
        public static final int DEFAULT_READ_TIMEOUT = 30000;    // 30 seconds
        /** Quick timeout in milliseconds for time-sensitive operations (5 seconds) */
        public static final int QUICK_TIMEOUT = 5000;            // 5 seconds
        /** Extended timeout in milliseconds for operations that might take longer (60 seconds) */
        public static final int EXTENDED_TIMEOUT = 60000;        // 60 seconds
    }

    /**
     * URL and encoding constants
     */
    public static final class Url {
        /** Path separator character for URLs */
        public static final String PATH_SEPARATOR = "/";
        /** Query string prefix for URLs */
        public static final String QUERY_PREFIX = "?";
        /** Query parameter separator for URLs */
        public static final String QUERY_SEPARATOR = "&";
        /** Parameter name-value separator for URLs */
        public static final String PARAM_EQUALS = "=";
        /** Fragment identifier prefix for URLs */
        public static final String FRAGMENT_PREFIX = "#";
    }

    /**
     * Encoding constants
     */
    public static final class Encoding {
        /** UTF-8 character encoding */
        public static final String UTF8 = "UTF-8";
        /** ISO-8859-1 character encoding */
        public static final String ISO_8859_1 = "ISO-8859-1";
    }

    /**
     * Common error messages
     */
    public static final class Errors {
        /** Error message for expired or invalid token */
        public static final String TOKEN_EXPIRED = "Token expired or invalid";
        /** Prefix for HTTP request failure message */
        public static final String REQUEST_FAILED = "HTTP request failed with status ";
        /** Error message for request execution failure */
        public static final String EXECUTE_REQUEST = "Failed to execute request";
        /** Error message template for parameter encoding failure */
        public static final String ENCODE_PARAM = "Failed to encode parameter: {}";
        /** Error message for SSL configuration issues */
        public static final String SSL_ERROR = "SSL configuration error";
        /** Error message for request timeout */
        public static final String TIMEOUT = "Request timed out";
    }

    /**
     * CircuitBreaker-related constants
     */
    public static final class CircuitBreaker {
        // Default values
        /** Default number of failures before opening the circuit */
        public static final int DEFAULT_FAILURE_THRESHOLD = 5;
        /** Default timeout in milliseconds before attempting to half-open the circuit (30 seconds) */
        public static final long DEFAULT_RESET_TIMEOUT_MS = 30000; // 30 seconds
        /** Default time in milliseconds before failure count begins to decay (1 minute) */
        public static final long DEFAULT_FAILURE_DECAY_TIME_MS = 60000; // 1 minute

        // Error messages
        /** Error message when the circuit is open */
        public static final String CIRCUIT_OPEN = "Circuit breaker is open";

        // Log messages
        /** Log message when circuit moves to half-open state */
        public static final String LOG_CIRCUIT_HALF_OPEN = "Circuit moving to HALF_OPEN state";
        /** Log message when circuit is closed */
        public static final String LOG_CIRCUIT_CLOSED = "Circuit closed - service appears healthy";
        /** Log message when test request fails */
        public static final String LOG_TEST_FAILED = "Test request failed, circuit remaining open";
        /** Log message template when circuit is opened */
        public static final String LOG_CIRCUIT_OPENED = "Circuit breaker opened after {} failures";
        /** Log message when failure count is reset due to decay timeout */
        public static final String LOG_DECAY_RESET = "Reset failure count due to decay timeout";
        /** Log message when a request is rejected during HALF_OPEN because another test is in progress */
        public static final String LOG_HALF_OPEN_TEST_IN_PROGRESS = "Rejecting request - another test request is in progress";

        // Health status
        /** Circuit breaker status: open (not allowing requests) */
        public static final String STATUS_OPEN = "OPEN";
        /** Circuit breaker status: closed (allowing requests) */
        public static final String STATUS_CLOSED = "CLOSED";
        /** Circuit breaker status: half-open (allowing test requests) */
        public static final String STATUS_HALF_OPEN = "HALF_OPEN";

        // Options
        /** Configuration option key for enabling/disabling circuit breaker */
        public static final String CIRCUIT_BREAKER_ENABLED = "circuit_breaker.enabled";
        /** Configuration option key for setting failure threshold */
        public static final String CIRCUIT_BREAKER_FAILURE_THRESHOLD = "circuit_breaker.failure_threshold";
        /** Configuration option key for setting reset timeout */
        public static final String CIRCUIT_BREAKER_RESET_TIMEOUT_MS = "circuit_breaker.reset_timeout_ms";
        /** Configuration option key for setting failure policy */
        public static final String CIRCUIT_BREAKER_FAILURE_POLICY = "circuit_breaker.failure_policy";
        /** Configuration option value for server-errors-only failure policy */
        public static final String CIRCUIT_BREAKER_POLICY_SERVER_ONLY = "SERVER_ERRORS_ONLY";
    }

    // Prevent instantiation
    private HttpConstants() {
        throw new AssertionError("Utility class - do not instantiate");
    }
} 