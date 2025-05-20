package com.guinetik.rr;

import com.guinetik.rr.api.ApiException;
import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.http.RocketRestException;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for RocketRestMock functionality
 */
public class RocketRestMockTest {

    private RocketRestMock mockClient;
    private static final String BASE_URL = "https://api.test.com";
    private static final String SUCCESS_ENDPOINT = "/success";
    private static final String ERROR_ENDPOINT = "/error";
    private static final String REGEX_ENDPOINT = "/users/\\d+";
    private static final String LATENCY_ENDPOINT = "/slow";

    @Before
    public void setUp() {
        // Create a basic configuration
        RocketRestConfig config = RocketRestConfig.builder(BASE_URL).build();
        mockClient = new RocketRestMock(config);

        // Set up mock responses
        mockClient.addMockResponse(HttpConstants.Methods.GET, SUCCESS_ENDPOINT, (url, body) -> "Success Response");
        mockClient.addMockResponse(HttpConstants.Methods.POST, SUCCESS_ENDPOINT, (url, body) -> "Post Success");
        mockClient.addMockResponse(HttpConstants.Methods.GET, ERROR_ENDPOINT, (url, body) -> {
            throw new ApiException("Internal Server Error", "Server Error Response", "Internal Server Error", 
                HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR);
        });
        mockClient.addMockResponse(HttpConstants.Methods.GET, REGEX_ENDPOINT, (url, body) -> "User Response", true);
    }

    @After
    public void tearDown() {
        mockClient.shutdown();
    }

    @Test
    public void testBasicMockResponses() {
        // Test successful GET
        String response = mockClient.get(SUCCESS_ENDPOINT, String.class);
        assertEquals("Success Response", response);

        // Test successful POST
        response = mockClient.post(SUCCESS_ENDPOINT, String.class);
        assertEquals("Post Success", response);

        // Test error response
        try {
            mockClient.get(ERROR_ENDPOINT, String.class);
            fail("Should have thrown an exception");
        } catch (RocketRestException e) {
            assertEquals(HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR, e.getStatusCode());
            assertEquals("Internal Server Error", e.getMessage());
        }
    }

    @Test
    public void testRegexPatternMatching() {
        // Test regex pattern matching
        String response = mockClient.get("/users/123", String.class);
        assertEquals("User Response", response);

        response = mockClient.get("/users/456", String.class);
        assertEquals("User Response", response);

        // Test non-matching URL
        try {
            mockClient.get("/users/abc", String.class);
            fail("Should have thrown an exception");
        } catch (RocketRestException e) {
            assertTrue(e.getMessage().contains("No mock response configured"));
        }
    }

    @Test
    public void testInvocationCounting() {
        // Make some requests
        mockClient.get(SUCCESS_ENDPOINT, String.class);
        mockClient.get(SUCCESS_ENDPOINT, String.class);
        mockClient.post(SUCCESS_ENDPOINT, String.class);

        // Verify invocation counts
        assertEquals(2, mockClient.getInvocationCount(HttpConstants.Methods.GET, SUCCESS_ENDPOINT));
        assertEquals(1, mockClient.getInvocationCount(HttpConstants.Methods.POST, SUCCESS_ENDPOINT));
        assertEquals(0, mockClient.getInvocationCount(HttpConstants.Methods.GET, "/nonexistent"));

        // Test regex pattern invocation counting
        mockClient.get("/users/123", String.class);
        mockClient.get("/users/456", String.class);
        assertEquals(2, mockClient.getInvocationCount(HttpConstants.Methods.GET, REGEX_ENDPOINT));

        // Test reset
        mockClient.resetInvocationCounts();
        assertEquals(0, mockClient.getInvocationCount(HttpConstants.Methods.GET, SUCCESS_ENDPOINT));
        assertEquals(0, mockClient.getInvocationCount(HttpConstants.Methods.POST, SUCCESS_ENDPOINT));
        assertEquals(0, mockClient.getInvocationCount(HttpConstants.Methods.GET, REGEX_ENDPOINT));
    }

    @Test
    public void testLatencySimulation() {
        // Add latency for a specific endpoint
        long latencyMs = HttpConstants.Timeouts.QUICK_TIMEOUT;
        mockClient.withLatency(LATENCY_ENDPOINT, latencyMs);
        mockClient.addMockResponse(HttpConstants.Methods.GET, LATENCY_ENDPOINT, (url, body) -> "Delayed Response");

        // Measure execution time
        long startTime = System.nanoTime();
        String response = mockClient.get(LATENCY_ENDPOINT, String.class);
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Verify response and timing
        assertEquals("Delayed Response", response);
        assertTrue("Request should take at least the configured latency",
                durationMs >= latencyMs);
    }

    @Test
    public void testFluentApiWithMock() {
        // Test successful request
        Result<String, ApiError> successResult = mockClient.fluent().get(SUCCESS_ENDPOINT, String.class);
        assertTrue(successResult.isSuccess());
        assertEquals("Success Response", successResult.getValue());

        // Test error request
        Result<String, ApiError> errorResult = mockClient.fluent().get(ERROR_ENDPOINT, String.class);
        assertTrue(errorResult.isFailure());
        ApiError error = errorResult.getError();
        assertEquals(ApiError.ErrorType.HTTP_ERROR, error.getErrorType());
        assertEquals(HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR, error.getStatusCode());
    }

    @Test
    public void testAsyncApiWithMock() {
        // Test successful async request
        String response = mockClient.async()
                .get(SUCCESS_ENDPOINT, String.class)
                .join();
        assertEquals("Success Response", response);

        // Test error async request
        try {
            mockClient.async()
                    .get(ERROR_ENDPOINT, String.class)
                    .join();
            fail("Should have thrown an exception");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RocketRestException);
            RocketRestException re = (RocketRestException) e.getCause();
            assertEquals(HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR, re.getStatusCode());
        }
    }

    @Test
    public void testMultipleLatencyPatterns() {
        // Add different latencies for different patterns
        mockClient.withLatency("/slow/.*", HttpConstants.Timeouts.QUICK_TIMEOUT);
        mockClient.withLatency("/very/slow/.*", HttpConstants.Timeouts.DEFAULT_CONNECT_TIMEOUT);
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/slow/test", (url, body) -> "Slow Response");
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/very/slow/test", (url, body) -> "Very Slow Response");

        // Test first pattern
        long startTime = System.nanoTime();
        String response = mockClient.get("/slow/test", String.class);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        assertEquals("Slow Response", response);
        assertTrue("Should take at least 100ms", durationMs >= 100);

        // Test second pattern
        startTime = System.nanoTime();
        response = mockClient.get("/very/slow/test", String.class);
        durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        assertEquals("Very Slow Response", response);
        assertTrue("Should take at least 200ms", durationMs >= HttpConstants.Timeouts.DEFAULT_CONNECT_TIMEOUT);
    }

    @Test
    public void testResponseTypeConversion() {
        // Test response type conversion using ObjectMapper
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/number", (url, body) -> 42);
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/boolean", (url, body) -> true);
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/map", (url, body) -> Collections.singletonMap("key", "value"));

        // Test number conversion
        Integer number = mockClient.get("/number", Integer.class);
        assertEquals(Integer.valueOf(42), number);

        // Test boolean conversion
        Boolean bool = mockClient.get("/boolean", Boolean.class);
        assertTrue(bool);

        // Test map conversion
        @SuppressWarnings("unchecked")
        Map<String, String> map = mockClient.get("/map", Map.class);
        assertEquals("value", map.get("key"));
    }
} 