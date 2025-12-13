package com.guinetik.rr.result;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ApiError}.
 */
public class ApiErrorTest {

    @Test
    public void testHttpError() {
        ApiError error = ApiError.httpError("Not Found", 404, "{\"error\":\"not found\"}");

        assertEquals("Not Found", error.getMessage());
        assertEquals(404, error.getStatusCode());
        assertEquals("{\"error\":\"not found\"}", error.getResponseBody());
        assertEquals(ApiError.ErrorType.HTTP_ERROR, error.getErrorType());
    }

    @Test
    public void testNetworkError() {
        ApiError error = ApiError.networkError("Connection refused");

        assertEquals("Connection refused", error.getMessage());
        assertEquals(0, error.getStatusCode());
        assertNull(error.getResponseBody());
        assertEquals(ApiError.ErrorType.NETWORK_ERROR, error.getErrorType());
    }

    @Test
    public void testCircuitOpenError() {
        ApiError error = ApiError.circuitOpenError("Circuit breaker is open");

        assertEquals("Circuit breaker is open", error.getMessage());
        assertEquals(0, error.getStatusCode());
        assertNull(error.getResponseBody());
        assertEquals(ApiError.ErrorType.CIRCUIT_OPEN, error.getErrorType());
    }

    @Test
    public void testParseError() {
        ApiError error = ApiError.parseError("Invalid JSON response", "{invalid}");

        assertEquals("Invalid JSON response", error.getMessage());
        assertEquals(0, error.getStatusCode());
        assertEquals("{invalid}", error.getResponseBody());
        assertEquals(ApiError.ErrorType.PARSE_ERROR, error.getErrorType());
    }

    @Test
    public void testAuthError() {
        ApiError error = ApiError.authError("Unauthorized", 401, "{\"error\":\"invalid token\"}");

        assertEquals("Unauthorized", error.getMessage());
        assertEquals(401, error.getStatusCode());
        assertEquals("{\"error\":\"invalid token\"}", error.getResponseBody());
        assertEquals(ApiError.ErrorType.AUTH_ERROR, error.getErrorType());
    }

    @Test
    public void testConfigError() {
        ApiError error = ApiError.configError("Invalid configuration");

        assertEquals("Invalid configuration", error.getMessage());
        assertEquals(0, error.getStatusCode());
        assertNull(error.getResponseBody());
        assertEquals(ApiError.ErrorType.CONFIG_ERROR, error.getErrorType());
    }

    @Test
    public void testConstructor() {
        ApiError error = new ApiError("Custom error", 500, "Server Error", ApiError.ErrorType.HTTP_ERROR);

        assertEquals("Custom error", error.getMessage());
        assertEquals(500, error.getStatusCode());
        assertEquals("Server Error", error.getResponseBody());
        assertEquals(ApiError.ErrorType.HTTP_ERROR, error.getErrorType());
    }

    @Test
    public void testHttpErrorWithNullBody() {
        ApiError error = ApiError.httpError("Bad Request", 400, null);

        assertEquals("Bad Request", error.getMessage());
        assertEquals(400, error.getStatusCode());
        assertNull(error.getResponseBody());
    }

    @Test
    public void testIsType() {
        ApiError httpError = ApiError.httpError("Error", 500, null);
        ApiError networkError = ApiError.networkError("Network");

        assertTrue(httpError.isType(ApiError.ErrorType.HTTP_ERROR));
        assertFalse(httpError.isType(ApiError.ErrorType.NETWORK_ERROR));
        assertTrue(networkError.isType(ApiError.ErrorType.NETWORK_ERROR));
        assertFalse(networkError.isType(ApiError.ErrorType.HTTP_ERROR));
    }

    @Test
    public void testHasStatusCode() {
        ApiError error404 = ApiError.httpError("Not Found", 404, null);
        ApiError error500 = ApiError.httpError("Server Error", 500, null);

        assertTrue(error404.hasStatusCode(404));
        assertFalse(error404.hasStatusCode(500));
        assertTrue(error500.hasStatusCode(500));
        assertFalse(error500.hasStatusCode(404));
    }

    @Test
    public void testErrorTypeValues() {
        ApiError.ErrorType[] types = ApiError.ErrorType.values();
        assertTrue(types.length >= 6);
        assertNotNull(ApiError.ErrorType.valueOf("HTTP_ERROR"));
        assertNotNull(ApiError.ErrorType.valueOf("NETWORK_ERROR"));
        assertNotNull(ApiError.ErrorType.valueOf("CIRCUIT_OPEN"));
        assertNotNull(ApiError.ErrorType.valueOf("PARSE_ERROR"));
        assertNotNull(ApiError.ErrorType.valueOf("AUTH_ERROR"));
        assertNotNull(ApiError.ErrorType.valueOf("CONFIG_ERROR"));
    }

    @Test
    public void testToString() {
        ApiError error = ApiError.httpError("Not Found", 404, "{\"error\":\"not found\"}");
        String str = error.toString();

        // toString should contain key information
        assertNotNull(str);
        assertTrue(str.contains("404") || str.contains("Not Found") || str.contains("HTTP_ERROR"));
    }

    @Test
    public void testToStringWithoutStatusCode() {
        ApiError error = ApiError.networkError("Connection refused");
        String str = error.toString();

        assertNotNull(str);
        assertTrue(str.contains("Connection refused") || str.contains("NETWORK_ERROR"));
    }
}
