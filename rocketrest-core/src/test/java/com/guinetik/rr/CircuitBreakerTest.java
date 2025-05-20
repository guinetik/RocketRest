package com.guinetik.rr;

import com.guinetik.rr.api.ApiException;
import com.guinetik.rr.http.CircuitBreakerOpenException;
import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the Circuit Breaker functionality
 */
public class CircuitBreakerTest {

    private RocketRestMock mockClient;
    private static final int FAILURE_THRESHOLD = 3;
    private static final long RESET_TIMEOUT_MS = 1000; // 1 second for faster testing

    @Before
    public void setUp() {
        // Create configuration with circuit breaker settings
        RocketRestConfig config = RocketRestConfig.builder("https://api.test.com")
                .defaultOptions(options -> {
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_ENABLED, true);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_THRESHOLD, FAILURE_THRESHOLD);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_RESET_TIMEOUT_MS, RESET_TIMEOUT_MS);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_POLICY, HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_POLICY_SERVER_ONLY);
                })
                .build();
        
        // Create a mock client using the config
        mockClient = new RocketRestMock(config);
        
        // Set up a success response for /success endpoint
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/success", (url, body) -> "Success Response");
        
        // Set up a server error response that will throw a proper HTTP 500 error
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/error", (url, body) -> {
            throw new ApiException("Internal Server Error", "Server Error Response", "Internal Server Error", 
                HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR);
        });
    }
    
    @After
    public void tearDown() {
        mockClient.shutdown();
    }
    
    @Test
    public void testCircuitBreakerOpens() {
        // First, make a successful call
        String successResponse = mockClient.get("/success", String.class);
        assertEquals("Success Response", successResponse);
        
        // Make FAILURE_THRESHOLD - 1 failures
        for (int i = 0; i < FAILURE_THRESHOLD - 1; i++) {
            try {
                // This should fail
                mockClient.get("/error", String.class);
                fail("Should have thrown an exception");
            } catch (Exception e) {
                // Expected exception, verify it's not a circuit breaker open exception
                assertFalse("Should not be circuit breaker exception yet on failure #" + (i+1),
                        e instanceof CircuitBreakerOpenException);
            }
        }
        
        // The last failure might open the circuit or might still throw the regular exception
        // Either behavior is acceptable since it depends on timing
        try {
            mockClient.get("/error", String.class);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // Expected exception could be either regular or circuit breaker
        }
        
        // Add a small delay to ensure circuit breaker state updates
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Now the circuit should definitely be open, any further calls should throw CircuitBreakerOpenException
        try {
            mockClient.get("/success", String.class);
            fail("Should have thrown CircuitBreakerOpenException");
        } catch (Exception e) {
            // Verify the correct exception type
            assertTrue("Expected CircuitBreakerOpenException but got: " + e.getClass().getName(), 
                    e instanceof CircuitBreakerOpenException);
            
            // Verify the exception contains timing information
            CircuitBreakerOpenException cbException = (CircuitBreakerOpenException) e;
            assertTrue("Should have time until reset", cbException.getEstimatedMillisUntilReset() > 0);
        }
    }
    
    @Test
    public void testCircuitBreakerResetsAfterTimeout() throws Exception {
        // Make FAILURE_THRESHOLD - 1 failures
        for (int i = 0; i < FAILURE_THRESHOLD - 1; i++) {
            try {
                mockClient.get("/error", String.class);
            } catch (Exception e) {
                // Expected, ignore
            }
        }
        
        // The last failure that should trip the circuit breaker
        try {
            mockClient.get("/error", String.class);
        } catch (Exception e) {
            // Expected, ignore
        }
        
        // Add a small delay to ensure circuit breaker state updates
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify the circuit is open
        try {
            mockClient.get("/success", String.class);
            fail("Circuit should be open");
        } catch (CircuitBreakerOpenException e) {
            // Expected
        }
        
        // Wait for the reset timeout
        Thread.sleep(RESET_TIMEOUT_MS);
        
        // Now the circuit should be half-open, allowing one test request
        String response = mockClient.get("/success", String.class);
        assertEquals("Success Response", response);
        
        // Another successful call should work as the circuit is now closed
        response = mockClient.get("/success", String.class);
        assertEquals("Success Response", response);
    }
    
    @Test
    public void testCircuitBreakerWithFluentAPI() {
        // Make FAILURE_THRESHOLD - 1 failures
        for (int i = 0; i < FAILURE_THRESHOLD - 1; i++) {
            Result<String, ApiError> result = mockClient.fluent().get("/error", String.class);
            assertTrue("Should be a failure result", result.isFailure());
            ApiError error = result.getError();
            assertNotNull("Error should not be null", error);
            // These shouldn't be circuit open errors yet
            assertNotEquals("Should not be CIRCUIT_OPEN error type on request #" + (i+1),
                    ApiError.ErrorType.CIRCUIT_OPEN, error.getErrorType());
        }
        
        // The last failure might cause the circuit to open
        Result<String, ApiError> lastFailureResult = mockClient.fluent().get("/error", String.class);
        assertTrue("Should be a failure result", lastFailureResult.isFailure());
        
        // Add a small delay to ensure circuit breaker state updates
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Now the circuit should definitely be open
        Result<String, ApiError> result = mockClient.fluent().get("/success", String.class);
        
        assertTrue("Should be a failure result", result.isFailure());
        ApiError error = result.getError();
        assertNotNull("Error should not be null", error);
        assertEquals("Should be CIRCUIT_OPEN error type", 
                ApiError.ErrorType.CIRCUIT_OPEN, error.getErrorType());
    }
    
    @Test
    public void testCircuitBreakerIsDisabledByDefault() {
        // Create a mock client with default settings (circuit breaker disabled)
        RocketRestConfig config = RocketRestConfig.builder("https://api.test.com").build();
        RocketRestMock defaultClient = new RocketRestMock(config);
        
        try {
            // Setup mock response for the new client
            defaultClient.addMockResponse(HttpConstants.Methods.GET, "/error", (url, body) -> {
                throw new ApiException("Internal Server Error", "Server Error Response", "Internal Server Error", 
                    HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR);
            });
            
            // Even with multiple errors, the circuit should stay closed
            for (int i = 0; i < FAILURE_THRESHOLD + 2; i++) {
                try {
                    defaultClient.get("/error", String.class);
                } catch (CircuitBreakerOpenException e) {
                    fail("Circuit breaker should not open when disabled");
                } catch (Exception e) {
                    // Other exceptions are expected
                }
            }
        } finally {
            defaultClient.shutdown();
        }
    }
} 