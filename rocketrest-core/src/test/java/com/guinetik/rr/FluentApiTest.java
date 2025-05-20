package com.guinetik.rr;

import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Fluent API with Result pattern
 */
public class FluentApiTest {

    // Reusing the User class from RocketRestMockTest
    public static class User {
        private int id;
        private String name;
        private String email;
        
        public User() {}
        
        public User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    private RocketRestMock mockClient;
    
    @Before
    public void setUp() {
        // Initialize a mock client before each test
        RocketRestConfig config = RocketRestConfig.builder("https://mock-api.example.com").build();
        mockClient = new RocketRestMock(config);
        
        // Setup mock responses
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/users/1", (url, body) -> 
            new User(1, "Test User", "test@example.com"));
        
        // Setup an error response
        mockClient.addMockResponse(HttpConstants.Methods.GET, "/error", (url, body) -> {
            throw new RuntimeException("Not found");
        });
    }
    
    @Test
    public void testFluentSuccessfulResponse() {
        // Use the fluent API with Result pattern
        Result<User, ApiError> result = mockClient.fluent().get("/users/1", User.class);
        
        assertTrue("Result should be a success", result.isSuccess());
        assertFalse("Result should not be a failure", result.isFailure());
        
        User user = result.getValue();
        assertNotNull("User should not be null", user);
        assertEquals("User ID should match", 1, user.getId());
        assertEquals("User name should match", "Test User", user.getName());
    }
    
    @Test
    public void testFluentErrorResponse() {
        // Use the fluent API with Result pattern for error case
        Result<String, ApiError> result = mockClient.fluent().get("/error", String.class);
        
        assertFalse("Result should not be a success", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        ApiError error = result.getError();
        assertNotNull("Error should not be null", error);
        assertNotNull("Error message should not be null", error.getMessage());
    }
    
    @Test
    public void testResultPatternHandling() {
        // Demonstrate handling both success and failure cases with the Result pattern
        Result<User, ApiError> result = mockClient.fluent().get("/users/1", User.class);
        
        // Method 1: Using isSuccess/isFailure checks
        if (result.isSuccess()) {
            User user = result.getValue();
            assertNotNull("User should not be null", user);
        } else {
            fail("Should be a success result");
        }
        
        // Method 2: Using ifSuccess/ifFailure callbacks
        final boolean[] successCallbackCalled = {false};
        final boolean[] failureCallbackCalled = {false};
        
        result
            .ifSuccess(user -> {
                successCallbackCalled[0] = true;
                assertEquals("User ID should be 1", 1, user.getId());
            })
            .ifFailure(error -> {
                failureCallbackCalled[0] = true;
            });
        
        assertTrue("Success callback should have been called", successCallbackCalled[0]);
        assertFalse("Failure callback should not have been called", failureCallbackCalled[0]);
    }
} 