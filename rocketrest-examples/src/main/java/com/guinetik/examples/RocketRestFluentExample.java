package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;

/**
 * Example demonstrating the Fluent API interface with Result pattern 
 * using the main RocketRest class.
 */
public class RocketRestFluentExample implements Example {
    
    private static final String API_BASE_URL = "https://jsonplaceholder.typicode.com";
    
    @Override
    public String getName() {
        return "RocketRest Fluent API with Result Pattern";
    }
    
    @Override
    public void run() {
        System.out.println("Demonstrating the RocketRest Fluent API with Result pattern...");
        
        // Create configuration for the API
        RocketRestConfig config = RocketRestConfig.builder(API_BASE_URL)
                .build();
        
        // Create RocketRest client 
        RocketRest client = new RocketRest(API_BASE_URL, config);
        
        try {
            // Example 1: Successful request with pattern matching style
            System.out.println("\n=== Example 1: GET Todo (Pattern Matching Style) ===");
            Result<Todo, ApiError> todoResult = client.fluent().get("/todos/1", Todo.class);
            
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
            client.fluent().get("/todos/2", Todo.class)
                .ifSuccess(todo -> System.out.println("Todo title: " + todo.getTitle()))
                .ifFailure(error -> System.out.println("Error: " + error.getMessage()));
            
            // Example 3: Handling an error case
            System.out.println("\n=== Example 3: GET Non-existent Resource (Error Handling) ===");
            Result<Todo, ApiError> errorResult = client.fluent().get("/todos/999", Todo.class);
            
            Todo todo = errorResult.getOrElse(new Todo(0, 0, "Default todo (when error occurs)", false));
            System.out.println("Got todo (with fallback): " + todo);
            
            // Example 4: Mapping result
            System.out.println("\n=== Example 4: GET with Result Mapping ===");
            String title = client.fluent().get("/todos/3", Todo.class)
                .map(t -> "TITLE: " + t.getTitle())
                .getOrElse("No title available");
            
            System.out.println(title);
            
            // Example 5: Direct access method vs. fluent API comparison
            System.out.println("\n=== Example 5: Comparing direct access vs. fluent API ===");
            try {
                // Direct access method (throws exceptions)
                Todo directTodo = client.get("/todos/4", Todo.class);
                System.out.println("Direct access successful: " + directTodo.getTitle());
                
                // Equivalent fluent API call (returns Result)
                Result<Todo, ApiError> fluentResult = client.fluent().get("/todos/4", Todo.class);
                fluentResult.ifSuccess(t -> System.out.println("Fluent API successful: " + t.getTitle()));
            } catch (Exception e) {
                System.out.println("Direct access threw exception: " + e.getMessage());
            }
            
            // Example 6: Non-existent endpoint
            System.out.println("\n=== Example 6: Handling a 404 Error ===");
            client.fluent().get("/non-existent", String.class)
                .ifSuccess(response -> System.out.println("This shouldn't happen!"))
                .ifFailure(error -> {
                    System.out.println("Expected error occurred: " + error);
                    if (error.getStatusCode() == 404) {
                        System.out.println("Confirmed 404 Not Found error");
                    }
                });
                
            // Example 7: POST request with body
            System.out.println("\n=== Example 7: POST Request with Body ===");
            Todo newTodo = new Todo(0, 1, "My new todo item", false);
            client.fluent().post("/todos", newTodo, Todo.class)
                .ifSuccess(created -> System.out.println("Successfully created Todo with ID: " + created.getId()))
                .ifFailure(error -> System.out.println("Failed to create Todo: " + error));
                
        } finally {
            client.shutdown();
        }
    }
} 