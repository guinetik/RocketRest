package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.json.JsonObjectMapper;

/**
 * Example that demonstrates fetching a Todo from JSONPlaceholder API
 */
public class JsonTodoExample implements Example {
    
    @Override
    public String getName() {
        return "JSONPlaceholder Todo Example";
    }
    
    @Override
    public void run() {
        // Create configuration for the API
        RocketRestConfig config = RocketRestConfig.builder("https://jsonplaceholder.typicode.com")
                .build();

        // Create RocketRest client
        RocketRest rocketrest = new RocketRest(config);

        try {
            System.out.println("Fetching Todo from JSONPlaceholder API...");
            
            // Perform GET request and deserialize to Todo class
            Todo todo = rocketrest.get("/todos/1", Todo.class);
            
            // Print the result
            System.out.println("Successfully fetched Todo:");
            System.out.println(JsonObjectMapper.toJsonString(todo));
            
        } catch (Exception e) {
            System.err.println("Error fetching data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always shutdown the client to release resources
            rocketrest.shutdown();
        }
    }
} 