package com.guinetik.rr.util;

import com.guinetik.rr.RocketRestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for configurable HTTP response logging.
 *
 * <p>This class provides methods for logging HTTP response details based on
 * {@link RocketRestOptions} configuration. It supports logging status codes,
 * headers, and response bodies with configurable truncation for large responses.
 *
 * <h2>Logging Configuration</h2>
 * <pre class="language-java"><code>
 * RocketRestOptions options = new RocketRestOptions();
 *
 * // Enable/disable logging
 * options.set(RocketRestOptions.LOGGING_ENABLED, true);
 *
 * // Enable raw response logging (status + headers)
 * options.set(RocketRestOptions.LOG_RAW_RESPONSE, true);
 *
 * // Enable body logging
 * options.set(RocketRestOptions.LOG_RESPONSE_BODY, true);
 *
 * // Set max body length before truncation
 * options.set(RocketRestOptions.MAX_LOGGED_BODY_LENGTH, 4000);
 * </code></pre>
 *
 * <h2>Usage in HTTP Clients</h2>
 * <pre class="language-java"><code>
 * // After receiving response
 * Map&lt;String, String&gt; headers = parseHeaders(connection);
 * ResponseLogger.logRawResponse(statusCode, headers, options);
 *
 * String body = readResponseBody(connection);
 * ResponseLogger.logResponseBody(body, options);
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketRestOptions
 * @since 1.0.0
 */
public final class ResponseLogger {

    private static final Logger logger = LoggerFactory.getLogger(ResponseLogger.class);

    private ResponseLogger() {
        // Utility class, no instantiation
    }

    /**
     * Logs the raw HTTP response if enabled in the client options.
     *
     * @param statusCode    The HTTP status code
     * @param headers       The response headers
     * @param clientOptions The client options
     */
    public static void logRawResponse(int statusCode, java.util.Map<String, String> headers, RocketRestOptions clientOptions) {
        if (clientOptions.getBoolean(RocketRestOptions.LOG_RAW_RESPONSE, true) &&
                clientOptions.getBoolean(RocketRestOptions.LOGGING_ENABLED, true)) {
            logger.debug("HTTP Response: Status={}", statusCode);
            headers.forEach((key, value) -> logger.debug("Response Header: {}={}", key, value));
        }
    }

    /**
     * Logs the response body if enabled in the client options.
     *
     * @param responseBody  The response body as a string
     * @param clientOptions The client options
     */
    public static void logResponseBody(String responseBody, RocketRestOptions clientOptions) {
        if (clientOptions.getBoolean(RocketRestOptions.LOG_RESPONSE_BODY, false) &&
                clientOptions.getBoolean(RocketRestOptions.LOGGING_ENABLED, true) &&
                responseBody != null) {

            int maxLength = clientOptions.getInt(RocketRestOptions.MAX_LOGGED_BODY_LENGTH, 4000);

            if (responseBody.length() <= maxLength) {
                logger.debug("Response Body: {}", responseBody);
            } else {
                logger.debug("Response Body (truncated): {}...", responseBody.substring(0, maxLength));
                logger.debug("Response Body length: {}", responseBody.length());
            }
        }
    }
} 