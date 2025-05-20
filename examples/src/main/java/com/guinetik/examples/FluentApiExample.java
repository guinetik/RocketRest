package com.guinetik.examples;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.api.FluentApiClient;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;

/**
 * Example demonstrating the Result-based API for developers who prefer 
 * a more declarative approach without exceptions.
 */
public class FluentApiExample implements Example {
    
    private static final String API_BASE_URL = "https://jsonplaceholder.typicode.com";
    
    @Override
    public String getName() {
        return "Fluent API with Result Pattern";
    }
    
    @Override
    public void run() {
        System.out.println("Demonstrating the Result-based API (no exceptions)...");
        
        // Create configuration for the API
        RocketRestConfig config = RocketRestConfig.builder(API_BASE_URL)
                .build();
        
        // Create FluentApiClient that uses a Result pattern
        FluentApiClient client = new FluentApiClient(API_BASE_URL, config);
        
        try {
            // Example 1: Successful request with pattern matching style
            System.out.println("\n=== Example 1: GET Todo (Pattern Matching Style) ===");
            Result<Todo, ApiError> todoResult = client.get("/todos/1", Todo.class);
            
            if (todoResult.isSuccess()) {
                Todo todo = todoResult.getValue();
                System.out.println("Successfully fetched Todo: " + todo);
            } else {
                ApiError error = todoResult.getError();
                System.out.println("Failed to fetch Todo: " + error);
                System.out.println("Error type: " + error.getErrorType());
            }
            
            // Example 2: Using functional methods
            System.out.println("\n=== Example 2: GET Todo (Functional Style) ===");
            client.get("/todos/2", Todo.class)
                .ifSuccess(todo -> System.out.println("Todo title: " + todo.getTitle()))
                .ifFailure(error -> System.out.println("Error: " + error.getMessage()));
            
            // Example 3: Handling an error case
            System.out.println("\n=== Example 3: GET Non-existent Resource (Error Handling) ===");
            Result<Todo, ApiError> errorResult = client.get("/todos/999", Todo.class);
            
            Todo todo = errorResult.getOrElse(new Todo(0, 0, "Default todo (when error occurs)", false));
            System.out.println("Got todo (with fallback): " + todo);
            
            // Example 4: Mapping result
            System.out.println("\n=== Example 4: GET with Result Mapping ===");
            String title = client.get("/todos/3", Todo.class)
                .map(t -> "TITLE: " + t.getTitle())
                .getOrElse("No title available");
            
            System.out.println(title);
            
            // Example 5: Using String.class directly
            System.out.println("\n=== Example 5: GET Raw JSON ===");
            Result<String, ApiError> jsonResult = client.get("/users/1", String.class);
            
            jsonResult.ifSuccess(json -> {
                System.out.println("Raw JSON response:");
                System.out.println(json.substring(0, Math.min(json.length(), 200)) + "...");
            });
            
            // Example 6: Non-existent endpoint
            System.out.println("\n=== Example 6: Handling a 404 Error ===");
            client.get("/non-existent", String.class)
                .ifSuccess(response -> System.out.println("This shouldn't happen!"))
                .ifFailure(error -> {
                    System.out.println("Expected error occurred: " + error);
                    if (error.getStatusCode() == 404) {
                        System.out.println("Confirmed 404 Not Found error");
                    }
                });
                
        } finally {
            client.shutdown();
        }
    }
} 