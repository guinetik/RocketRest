package com.guinetik.rr.util;

import com.guinetik.rr.RocketRestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for logging HTTP responses.
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