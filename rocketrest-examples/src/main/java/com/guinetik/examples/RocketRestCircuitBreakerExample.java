package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.http.CircuitBreakerOpenException;
import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;

import java.time.LocalDateTime;

/**
 * Example demonstrating the Circuit Breaker pattern using RocketRest
 * with https://httpstat.us/ as a test service.
 */
public class RocketRestCircuitBreakerExample implements Example {
    
    private static final String API_BASE_URL = "https://httpstat.us";
    private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final long CIRCUIT_RESET_TIMEOUT_MS = 5000; // 5 seconds
    
    @Override
    public String getName() {
        return "RocketRest Circuit Breaker Pattern";
    }
    
    @Override
    public void run() {
        System.out.println("Demonstrating RocketRest with Circuit Breaker pattern...");
        
        // Create configuration for the API with circuit breaker settings
        RocketRestConfig config = RocketRestConfig.builder(API_BASE_URL)
                .defaultOptions(options -> {
                    // Configure circuit breaker options
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_ENABLED, true);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_THRESHOLD, CIRCUIT_FAILURE_THRESHOLD);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_RESET_TIMEOUT_MS, CIRCUIT_RESET_TIMEOUT_MS);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_POLICY, HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_POLICY_SERVER_ONLY);
                })
                .build();
        
        // Create RocketRest client with circuit breaker configured
        RocketRest client = new RocketRest(API_BASE_URL, config);
        
        try {
            // Basic circuit breaker demonstration
            demonstrateBasicCircuitBreaker(client);
            
            // Configure another client to show switching between 500 and 200
            demonstrateCircuitBreakerRecovery(client);
            
        } finally {
            client.shutdown();
        }
        
        System.out.println("\nCircuit Breaker example completed.");
    }
    
    /**
     * Demonstrates the circuit breaker pattern with a RocketRest client
     */
    private void demonstrateBasicCircuitBreaker(RocketRest client) {
        System.out.println("\n=== Basic Circuit Breaker Demonstration ===");
        
        // Make a successful request first
        System.out.println("\n1. Making successful requests (200 status)...");
        try {
            // Success cases
            for (int i = 0; i < 2; i++) {
                System.out.println(LocalDateTime.now() + " - Requesting 200 OK");
                String response = client.get("/200", String.class);
                System.out.println("✅ Success response: " + response);
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.out.println("❌ Unexpected error: " + e.getMessage());
        }
        
        // Now trigger the circuit breaker with error responses
        System.out.println("\n2. Triggering circuit breaker with 500 errors...");
        try {
            // Cause failures to trip the circuit breaker
            for (int i = 0; i < CIRCUIT_FAILURE_THRESHOLD + 1; i++) {
                try {
                    System.out.println(LocalDateTime.now() + " - Requesting 500 Internal Server Error");
                    // Use the fluent API to get a Result object for better error handling
                    Result<String, ApiError> result = client.fluent().get("/500", String.class);
                    
                    if (result.isSuccess()) {
                        System.out.println("✅ Got response: " + result.getValue());
                    } else {
                        ApiError error = result.getError();
                        System.out.println("❌ Error (failure #" + (i+1) + "): " + error.getMessage() + 
                                " (Status: " + error.getStatusCode() + ")");
                    }
                } catch (CircuitBreakerOpenException e) {
                    System.out.println("⚡ Circuit breaker is now OPEN: " + e.getMessage());
                    // The CircuitBreakerOpenException includes information about the reset timeout
                    long millisUntilReset = getTimeUntilReset(e);
                    System.out.println("Circuit will reset in approximately: " + millisUntilReset + "ms");
                    break;
                } catch (Exception e) {
                    System.out.println("❌ Error: " + e.getMessage());
                }
                
                Thread.sleep(500);
            }
            
            // Circuit should be open now, so requests should fail fast
            System.out.println("\n3. Testing fast-fail with circuit open...");
            for (int i = 0; i < 3; i++) {
                try {
                    System.out.println(LocalDateTime.now() + " - Attempting request when circuit is OPEN");
                    client.get("/200", String.class); // This should fail fast if circuit is open
                    System.out.println("✅ Request succeeded (circuit might be closed)");
                } catch (CircuitBreakerOpenException e) {
                    System.out.println("⚡ Fast fail! Circuit is still OPEN: " + e.getMessage());
                    long millisUntilReset = getTimeUntilReset(e);
                    System.out.println("Circuit will reset in approximately: " + millisUntilReset + "ms");
                } catch (Exception e) {
                    System.out.println("❌ Different error: " + e.getMessage());
                }
                
                Thread.sleep(1000);
            }
            
            // Wait for the circuit to enter half-open state
            System.out.println("\n4. Waiting for circuit reset timeout (" + CIRCUIT_RESET_TIMEOUT_MS + "ms)...");
            Thread.sleep(CIRCUIT_RESET_TIMEOUT_MS);
            
            // Now make a successful request to close the circuit
            System.out.println("\n5. Circuit should be HALF-OPEN now, trying to close it with successful requests...");
            for (int i = 0; i < 2; i++) {
                try {
                    System.out.println(LocalDateTime.now() + " - Requesting 200 OK to close circuit");
                    String response = client.get("/200", String.class);
                    System.out.println("✅ Success! Response: " + response);
                    System.out.println("Circuit should be CLOSED now");
                } catch (CircuitBreakerOpenException e) {
                    System.out.println("⚡ Circuit is still OPEN: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("❌ Error: " + e.getMessage());
                }
                
                Thread.sleep(500);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Demo interrupted");
        }
    }
    
    /**
     * Demonstrates alternating between 500 errors and 200 success to show the circuit breaker recovery
     */
    private void demonstrateCircuitBreakerRecovery(RocketRest client) {
        System.out.println("\n=== Circuit Breaker Recovery Demonstration ===");
        System.out.println("This example shows how the circuit breaker pattern helps with temporary service outages");
        
        try {
            // Step 1: Start with successful requests to ensure circuit is closed
            System.out.println("\n1. Starting with circuit CLOSED (successful requests)");
            for (int i = 0; i < 2; i++) {
                try {
                    String response = client.get("/200", String.class);
                    System.out.println("✅ Success response: " + response);
                } catch (Exception e) {
                    System.out.println("❌ Unexpected error: " + e.getMessage());
                }
                Thread.sleep(500);
            }
            
            // Step 2: Trigger failures to open the circuit
            System.out.println("\n2. Simulating a service outage with 500 errors");
            for (int i = 0; i < CIRCUIT_FAILURE_THRESHOLD; i++) {
                try {
                    client.get("/500", String.class);
                    System.out.println("Request somehow succeeded (unexpected)");
                } catch (CircuitBreakerOpenException e) {
                    System.out.println("⚡ Circuit breaker opened after " + (i+1) + " failures: " + e.getMessage());
                    break;
                } catch (Exception e) {
                    System.out.println("Error #" + (i+1) + ": " + e.getMessage());
                }
                Thread.sleep(500);
            }
            
            // Step 3: Verify that circuit is open by fast-failing
            System.out.println("\n3. Verifying circuit is OPEN (requests should fail fast)");
            try {
                client.get("/200", String.class);
                System.out.println("Circuit might not be open yet");
            } catch (CircuitBreakerOpenException e) {
                System.out.println("⚡ Circuit is open as expected: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("❌ Unexpected error type: " + e.getMessage());
            }
            
            // Step 4: Wait for reset timeout
            System.out.println("\n4. Waiting for circuit reset timeout to move to HALF-OPEN state...");
            Thread.sleep(CIRCUIT_RESET_TIMEOUT_MS);
            
            // Step 5: Service is now "recovered" (switch to 200 OK)
            System.out.println("\n5. Service has recovered (returning 200 OK)");
            try {
                String response = client.get("/200", String.class);
                System.out.println("✅ Success! Circuit should move from HALF-OPEN to CLOSED");
                System.out.println("Response: " + response);
            } catch (CircuitBreakerOpenException e) {
                System.out.println("⚡ Circuit is still open (unexpected): " + e.getMessage());
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
            
            // Step 6: Verify circuit is closed with more successful requests
            System.out.println("\n6. Verifying circuit is CLOSED with more requests");
            for (int i = 0; i < 3; i++) {
                try {
                    String response = client.get("/200", String.class);
                    System.out.println("✅ Success #" + (i+1) + ": " + response);
                } catch (Exception e) {
                    System.out.println("❌ Error (unexpected): " + e.getMessage());
                }
                Thread.sleep(500);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Demo interrupted");
        }
    }
    
    /**
     * Helper method to extract reset timeout information from the exception.
     */
    private long getTimeUntilReset(CircuitBreakerOpenException e) {
        // Use the existing getEstimatedMillisUntilReset method
        long timeUntilReset = e.getEstimatedMillisUntilReset();
        
        // If the existing method returns 0 (meaning it couldn't determine),
        // then fall back to our configured timeout
        return timeUntilReset > 0 ? timeUntilReset : CIRCUIT_RESET_TIMEOUT_MS;
    }
} 