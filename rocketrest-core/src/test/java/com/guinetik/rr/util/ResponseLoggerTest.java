package com.guinetik.rr.util;

import com.guinetik.rr.RocketRestOptions;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResponseLogger}.
 */
public class ResponseLoggerTest {

    private RocketRestOptions enabledOptions;
    private RocketRestOptions disabledOptions;
    private Map<String, String> testHeaders;

    @Before
    public void setUp() {
        enabledOptions = new RocketRestOptions();
        enabledOptions.set(RocketRestOptions.LOGGING_ENABLED, true);
        enabledOptions.set(RocketRestOptions.LOG_RAW_RESPONSE, true);
        enabledOptions.set(RocketRestOptions.LOG_RESPONSE_BODY, true);
        enabledOptions.set(RocketRestOptions.MAX_LOGGED_BODY_LENGTH, 4000);

        disabledOptions = new RocketRestOptions();
        disabledOptions.set(RocketRestOptions.LOGGING_ENABLED, false);

        testHeaders = new HashMap<>();
        testHeaders.put("Content-Type", "application/json");
        testHeaders.put("X-Request-Id", "123456");
    }

    @Test
    public void testLogRawResponseWithLoggingEnabled() {
        // Should not throw exception
        ResponseLogger.logRawResponse(200, testHeaders, enabledOptions);
    }

    @Test
    public void testLogRawResponseWithLoggingDisabled() {
        // Should not throw exception even when disabled
        ResponseLogger.logRawResponse(200, testHeaders, disabledOptions);
    }

    @Test
    public void testLogRawResponseWithEmptyHeaders() {
        Map<String, String> emptyHeaders = new HashMap<>();
        // Should not throw exception
        ResponseLogger.logRawResponse(404, emptyHeaders, enabledOptions);
    }

    @Test
    public void testLogResponseBodyWithLoggingEnabled() {
        String body = "{\"name\":\"John\",\"email\":\"john@example.com\"}";
        // Should not throw exception
        ResponseLogger.logResponseBody(body, enabledOptions);
    }

    @Test
    public void testLogResponseBodyWithLoggingDisabled() {
        String body = "{\"name\":\"John\"}";
        // Should not throw exception even when disabled
        ResponseLogger.logResponseBody(body, disabledOptions);
    }

    @Test
    public void testLogResponseBodyWithNullBody() {
        // Should not throw exception with null body
        ResponseLogger.logResponseBody(null, enabledOptions);
    }

    @Test
    public void testLogResponseBodyWithEmptyBody() {
        // Should not throw exception with empty body
        ResponseLogger.logResponseBody("", enabledOptions);
    }

    @Test
    public void testLogResponseBodyTruncation() {
        // Create a body longer than the max length
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("x");
        }
        String longBody = sb.toString();

        // Should not throw exception and should handle truncation
        ResponseLogger.logResponseBody(longBody, enabledOptions);
    }

    @Test
    public void testLogResponseBodyWithCustomMaxLength() {
        RocketRestOptions customOptions = new RocketRestOptions();
        customOptions.set(RocketRestOptions.LOGGING_ENABLED, true);
        customOptions.set(RocketRestOptions.LOG_RESPONSE_BODY, true);
        customOptions.set(RocketRestOptions.MAX_LOGGED_BODY_LENGTH, 100);

        String body = "Short body";
        // Should not throw exception
        ResponseLogger.logResponseBody(body, customOptions);
    }

    @Test
    public void testLogRawResponseWithDifferentStatusCodes() {
        // Test various status codes
        ResponseLogger.logRawResponse(200, testHeaders, enabledOptions);
        ResponseLogger.logRawResponse(201, testHeaders, enabledOptions);
        ResponseLogger.logRawResponse(400, testHeaders, enabledOptions);
        ResponseLogger.logRawResponse(401, testHeaders, enabledOptions);
        ResponseLogger.logRawResponse(404, testHeaders, enabledOptions);
        ResponseLogger.logRawResponse(500, testHeaders, enabledOptions);
    }

    @Test
    public void testLogWithOnlyLoggingEnabledFalse() {
        RocketRestOptions opts = new RocketRestOptions();
        opts.set(RocketRestOptions.LOGGING_ENABLED, false);
        opts.set(RocketRestOptions.LOG_RAW_RESPONSE, true);
        opts.set(RocketRestOptions.LOG_RESPONSE_BODY, true);

        // Even with raw and body enabled, if logging is disabled, nothing should happen
        ResponseLogger.logRawResponse(200, testHeaders, opts);
        ResponseLogger.logResponseBody("body", opts);
    }

    @Test
    public void testLogWithDefaultOptions() {
        RocketRestOptions defaultOpts = new RocketRestOptions();

        // Should work with default options
        ResponseLogger.logRawResponse(200, testHeaders, defaultOpts);
        ResponseLogger.logResponseBody("body", defaultOpts);
    }
}
