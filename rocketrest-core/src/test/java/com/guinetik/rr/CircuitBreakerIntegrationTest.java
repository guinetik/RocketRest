package com.guinetik.rr;

import com.guinetik.rr.http.CircuitBreakerOpenException;
import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration test for the Circuit Breaker functionality using a real HTTP client
 * against httpstat.us service.
 * Note: This test makes real HTTP calls and depends on an external service.
 */
public class CircuitBreakerIntegrationTest {

    private RocketRest client;
    private static final String API_BASE_URL = "https://httpstat.us";
    private static final int FAILURE_THRESHOLD = 3;
    private static final long RESET_TIMEOUT_MS = HttpConstants.Timeouts.QUICK_TIMEOUT; // 5 seconds

    @Before
    public void setUp() {
        // Create configuration for the API with circuit breaker settings
        RocketRestConfig config = RocketRestConfig.builder(API_BASE_URL)
                .defaultOptions(options -> {
                    // Configure circuit breaker options
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_ENABLED, true);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_THRESHOLD, FAILURE_THRESHOLD);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_RESET_TIMEOUT_MS, RESET_TIMEOUT_MS);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_POLICY, HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_POLICY_SERVER_ONLY);
                })
                .build();
        
        // Create a real client using the config
        client = new RocketRest(config);
    }
    
    @After
    public void tearDown() {
        client.shutdown();
    }
    
    @Test
    public void testSuccessfulRequest() {
        // Test a successful request first
        String response = client.get("/200", String.class);
        
        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain 200", response.contains("200"));
    }
    
    @Test
    public void testCircuitBreakerOpens() {
        // First, make a successful call
        String successResponse = client.get("/200", String.class);
        assertNotNull("Success response should not be null", successResponse);
        
        // Now trigger enough failures to open the circuit
        for (int i = 0; i < FAILURE_THRESHOLD - 1; i++) {
            try {
                // Request 500 status to trigger errors
                client.get("/500", String.class);
                // We should still get a response but as an error
            } catch (Exception e) {
                // Expected exception, but should not be circuit breaker yet
                assertFalse("Should not be circuit breaker exception on failure #" + (i+1),
                        e instanceof CircuitBreakerOpenException);
            }
        }
        
        // The last failure should open the circuit
        try {
            client.get("/500", String.class);
            fail("Should have thrown CircuitBreakerOpenException");
        } catch (Exception e) {
            // Verify the correct exception type
            assertTrue("Expected CircuitBreakerOpenException but got: " + e.getClass().getName(), 
                    e instanceof CircuitBreakerOpenException);
            
            // Verify the exception contains timing information
            CircuitBreakerOpenException cbException = (CircuitBreakerOpenException) e;
            assertTrue("Should have time until reset", cbException.getEstimatedMillisUntilReset() > 0);
        }
        
        // Now the circuit should be open, any further calls should throw CircuitBreakerOpenException
        try {
            client.get("/200", String.class);
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
    public void testCircuitBreakerWithFluentAPI() {
        // Trigger failures to reach (but not exceed) the threshold
        for (int i = 0; i < FAILURE_THRESHOLD - 1; i++) {
            Result<String, ApiError> result = client.fluent().get("/500", String.class);
            assertTrue("Should be a failure result for 500 status", result.isFailure());
            
            ApiError error = result.getError();
            assertEquals("Should be HTTP_ERROR error type on request #" + (i+1),
                    ApiError.ErrorType.HTTP_ERROR, error.getErrorType());
            assertEquals("Status code should be 500 on request #" + (i+1),
                    HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR, error.getStatusCode());
        }
        
        // The last failure might either be HTTP_ERROR or CIRCUIT_OPEN
        // depending on timing and exactly when the circuit opens
        Result<String, ApiError> lastFailureResult = client.fluent().get("/500", String.class);
        assertTrue("Should be a failure result", lastFailureResult.isFailure());
        
        // After reaching FAILURE_THRESHOLD, make sure circuit is definitely open
        // with a small delay to ensure circuit breaker state propagation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Now the circuit should definitely be open
        Result<String, ApiError> circuitOpenResult = client.fluent().get("/200", String.class);
        
        assertTrue("Should be a failure result due to open circuit", circuitOpenResult.isFailure());
        ApiError error = circuitOpenResult.getError();
        assertNotNull("Error should not be null", error);
        assertEquals("Should be CIRCUIT_OPEN error type", 
                ApiError.ErrorType.CIRCUIT_OPEN, error.getErrorType());
        // CircuitBreakerOpenException errors don't have a status code (it's 0)
        assertEquals("Circuit breaker errors should have status code 0", 0, error.getStatusCode());
    }
    
    /**
     * This test is for manual verification as it depends on timing.
     * Comment out the @Test annotation if you don't want to run it as part of automated tests.
     */
    @Test
    public void testCircuitBreakerResetsAfterTimeout() throws Exception {
        // Trigger enough failures to open the circuit
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            try {
                client.get("/500", String.class);
            } catch (Exception e) {
                // Expected exceptions from 500 responses
            }
        }
        
        // Verify the circuit is open
        try {
            client.get("/200", String.class);
            fail("Circuit should be open");
        } catch (CircuitBreakerOpenException e) {
            // Expected - circuit is open
            System.out.println("Circuit breaker is open as expected. Waiting for reset...");
        }
        
        // Wait for the reset timeout
        System.out.println("Waiting " + RESET_TIMEOUT_MS + "ms for circuit reset...");
        Thread.sleep(RESET_TIMEOUT_MS + 1000); // Add a bit extra for safety
        
        // Now the circuit should be half-open, allowing one test request
        try {
            String response = client.get("/200", String.class);
            System.out.println("Circuit is half-open or closed. Successful response: " + response);
            assertNotNull("Response should not be null after reset", response);
            
            // Another successful call should work as the circuit is now closed
            response = client.get("/200", String.class);
            System.out.println("Circuit is fully closed. Successful response: " + response);
            assertNotNull("Second response should not be null", response);
        } catch (CircuitBreakerOpenException e) {
            fail("Circuit should have reset after timeout, but got: " + e.getMessage());
        }
    }
} 