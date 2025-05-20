package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestConfig;

/**
 * Example that demonstrates fetching data from multiple endpoints of a Fake-JSON API
 */
public class FakeJsonApiExample implements Example {
    
    private static final String FAKE_API_BASE_URL = "https://fake-json-api.mock.beeceptor.com";
    private static final String DUMMY_API_BASE_URL = "https://dummy-json.mock.beeceptor.com";
    
    @Override
    public String getName() {
        return "Fake-JSON API Example";
    }
    
    @Override
    public void run() {
        // Create RocketRest client for fake-json-api
        RocketRestConfig fakeApiConfig = RocketRestConfig.builder(FAKE_API_BASE_URL)
                .build();
        RocketRest fakeApiClient = new RocketRest(fakeApiConfig);
        
        // Create RocketRest client for dummy-json-api
        RocketRestConfig dummyApiConfig = RocketRestConfig.builder(DUMMY_API_BASE_URL)
                .build();
        RocketRest dummyApiClient = new RocketRest(dummyApiConfig);
        
        try {
            // Fetch and display users
            System.out.println("\n=== USERS ===");
            System.out.println("Fetching users from " + FAKE_API_BASE_URL + "/users");
            String usersResponse = fakeApiClient.get("/users", String.class);
            System.out.println("Response:");
            System.out.println(usersResponse);
            
            // Fetch and display companies
            System.out.println("\n=== COMPANIES ===");
            System.out.println("Fetching companies from " + FAKE_API_BASE_URL + "/companies");
            String companiesResponse = fakeApiClient.get("/companies", String.class);
            System.out.println("Response:");
            System.out.println(companiesResponse);
            
            // Fetch and display posts
            System.out.println("\n=== POSTS ===");
            System.out.println("Fetching posts from " + DUMMY_API_BASE_URL + "/posts");
            String postsResponse = dummyApiClient.get("/posts", String.class);
            System.out.println("Response:");
            System.out.println(postsResponse);
            
            // Demonstrate usage with empty base URL (alternative approach)
            System.out.println("\n=== ALTERNATIVE APPROACH ===");
            RocketRestConfig emptyConfig = RocketRestConfig.builder("")
                    .build();
            RocketRest directClient = new RocketRest(emptyConfig);
            
            System.out.println("Fetching users with absolute URL");
            String directResponse = directClient.get(FAKE_API_BASE_URL + "/users", String.class);
            System.out.println("Response:");
            System.out.println(directResponse);
            directClient.shutdown();
            
        } catch (Exception e) {
            System.err.println("Error fetching data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always shutdown the clients to release resources
            fakeApiClient.shutdown();
            dummyApiClient.shutdown();
        }
    }
} 