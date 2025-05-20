package com.guinetik.rr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.request.RequestBuilder;
import com.guinetik.rr.request.RequestSpec;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for RocketRest core functionality
 */
public class RocketRestTest {
    
    private RocketRest client;
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    private static final String MOCK_API_URL = "https://682ac115ab2b5004cb3794c5.mockapi.io/api/v1";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Before
    public void setUp() {
        // Initialize a test client before each test
        RocketRestConfig config = RocketRestConfig.builder(BASE_URL).build();
        client = new RocketRest(config);
    }
    
    @After
    public void tearDown() {
        client.shutdown();
    }
    
    @Test
    public void testClientInitialization() {
        assertNotNull("Client should be initialized", client);
        
        // Test that sync, async, and fluent APIs are available
        assertNotNull("Sync API should be available", client.sync());
        assertNotNull("Async API should be available", client.async());
        assertNotNull("Fluent API should be available", client.fluent());
    }
    
    @Test
    public void testClientConfiguration() {
        // Test client configuration with options
        RocketRestConfig config = RocketRestConfig.builder(BASE_URL)
                .defaultOption(RocketRestOptions.LOGGING_ENABLED, true)
                .defaultOption(RocketRestOptions.LOG_RAW_RESPONSE, true)
                .build();
        
        RocketRest configuredClient = new RocketRest(config);
        assertNotNull("Configured client should be initialized", configuredClient);
        configuredClient.shutdown();
    }

    @Test
    public void testSyncApiMethods() throws Exception {
        // Set up the mock API endpoint
        client.setBaseUrl(MOCK_API_URL);
        
        // 1. Get all records and verify count
        String allRecords = client.sync().get("/records", String.class);
        assertNotNull("All records response should not be null", allRecords);
        List<Map<String, Object>> records = objectMapper.readValue(allRecords, List.class);
        assertEquals("Should have 5 records initially", 5, records.size());
        
        // 2. Verify first record name is Guinetik
        Map<String, Object> firstRecord = records.get(0);
        assertEquals("First record should have name Guinetik", "Guinetik", firstRecord.get("name"));
        
        // 3. Add new record
        Map<String, String> newRecord = new HashMap<>();
        newRecord.put("name", "Hello from Integrated Test");
        
        String postResponse = client.sync().post("/records", newRecord, String.class);
        assertNotNull("Post response should not be null", postResponse);
        Map<String, Object> postedData = objectMapper.readValue(postResponse, Map.class);
        String newRecordId = postedData.get("id").toString();
        assertEquals("Posted record should have correct name", "Hello from Integrated Test", postedData.get("name"));
        
        // 4. Update the new record
        Map<String, String> updatedRecord = new HashMap<>();
        updatedRecord.put("name", "Updated Name");
        
        String putResponse = client.sync().put("/records/" + newRecordId, updatedRecord, String.class);
        assertNotNull("Put response should not be null", putResponse);
        Map<String, Object> updatedData = objectMapper.readValue(putResponse, Map.class);
        assertEquals("Updated record should have new name", "Updated Name", updatedData.get("name"));
        
        // 5. Delete the record
        String deleteResponse = client.sync().delete("/records/" + newRecordId, String.class);
        assertNotNull("Delete response should not be null", deleteResponse);
        
        // 6. Get all records again and verify the count
        String finalRecords = client.sync().get("/records", String.class);
        assertNotNull("Final records response should not be null", finalRecords);
        List<Map<String, Object>> finalRecordsList = objectMapper.readValue(finalRecords, List.class);
        assertEquals("Should have 5 records after deletion", 5, finalRecordsList.size());
    }

    @Test
    public void testAsyncApiMethods() {
        // Test basic GET
        CompletableFuture<String> getFuture = client.async().get("/test", String.class);
        assertNotNull("GET future should be available", getFuture);
        
        // Test GET with query parameters
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("param1", "value1");
        CompletableFuture<String> getWithParamsFuture = client.async().get("/test", String.class, queryParams);
        assertNotNull("GET with query params future should be available", getWithParamsFuture);
        
        // Test POST without body
        CompletableFuture<String> postFuture = client.async().post("/test", String.class);
        assertNotNull("POST without body future should be available", postFuture);
        
        // Test POST with body
        String requestBody = "test body";
        CompletableFuture<String> postWithBodyFuture = client.async().post("/test", requestBody, String.class);
        assertNotNull("POST with body future should be available", postWithBodyFuture);
        
        // Test PUT without body
        CompletableFuture<String> putFuture = client.async().put("/test", String.class);
        assertNotNull("PUT without body future should be available", putFuture);
        
        // Test PUT with body
        CompletableFuture<String> putWithBodyFuture = client.async().put("/test", requestBody, String.class);
        assertNotNull("PUT with body future should be available", putWithBodyFuture);
        
        // Test DELETE
        CompletableFuture<String> deleteFuture = client.async().delete("/test", String.class);
        assertNotNull("DELETE future should be available", deleteFuture);
        
        // Test execute with RequestSpec
        RequestSpec<String, String> requestSpec = RequestBuilder.<String, String>get("/test")
            .body(requestBody)
            .responseType(String.class)
            .build();
        CompletableFuture<String> executeFuture = client.async().execute(requestSpec);
        assertNotNull("Execute with RequestSpec future should be available", executeFuture);
    }

    @Test
    public void testFluentApiMethods() {
        // Test basic GET
        Result<String, ApiError> getResult = client.fluent().get("/test", String.class);
        assertNotNull("GET result should be available", getResult);
        
        // Test GET with query parameters
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("param1", "value1");
        Result<String, ApiError> getWithParamsResult = client.fluent().get("/test", String.class, queryParams);
        assertNotNull("GET with query params result should be available", getWithParamsResult);
        
        // Test POST without body
        Result<String, ApiError> postResult = client.fluent().post("/test", String.class);
        assertNotNull("POST without body result should be available", postResult);
        
        // Test POST with body
        String requestBody = "test body";
        Result<String, ApiError> postWithBodyResult = client.fluent().post("/test", requestBody, String.class);
        assertNotNull("POST with body result should be available", postWithBodyResult);
        
        // Test PUT without body
        Result<String, ApiError> putResult = client.fluent().put("/test", String.class);
        assertNotNull("PUT without body result should be available", putResult);
        
        // Test PUT with body
        Result<String, ApiError> putWithBodyResult = client.fluent().put("/test", requestBody, String.class);
        assertNotNull("PUT with body result should be available", putWithBodyResult);
        
        // Test DELETE
        Result<String, ApiError> deleteResult = client.fluent().delete("/test", String.class);
        assertNotNull("DELETE result should be available", deleteResult);
        
        // Test execute with RequestSpec
        RequestSpec<String, String> requestSpec = RequestBuilder.<String, String>get("/test")
            .body(requestBody)
            .responseType(String.class)
            .build();
        Result<String, ApiError> executeResult = client.fluent().execute(requestSpec);
        assertNotNull("Execute with RequestSpec result should be available", executeResult);
    }

    @Test
    public void testClientConfigure() {
        // Test configuring client options
        client.configure(RocketRestOptions.LOGGING_ENABLED, true);
        client.configure(RocketRestOptions.LOG_RAW_RESPONSE, true);
        client.configure(RocketRestOptions.ASYNC_POOL_SIZE, 4);
        
        // Test that the client is still usable after configuration
        assertNotNull("Client should still work after configuration", 
            client.sync().get("/posts/1", String.class));
    }
} 