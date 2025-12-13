package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Example demonstrating how to fetch NASA's Astronomy Picture of the Day (APOD)
 * using RocketRest's fluent API.
 * API documentation: https://api.nasa.gov/
 */
public class NasaApodExample implements Example {
    
    private static final String NASA_API_BASE_URL = "https://api.nasa.gov";
    private static final String DEFAULT_API_KEY = "DEMO_KEY";
    
    @Override
    public String getName() {
        return "NASA Astronomy Picture of the Day (APOD) API";
    }
    
    @Override
    public void run() {
        System.out.println("====================================");
        System.out.println("NASA Astronomy Picture of the Day API");
        System.out.println("====================================");
        System.out.println("This example fetches the NASA APOD using RocketRest's fluent API.");
        System.out.println("The DEMO_KEY has strict usage limits. Get your free API key at: https://api.nasa.gov/");
        
        // Prompt for API key
        String apiKey = promptForApiKey();
        
        // Create RocketRest client for NASA API
        RocketRestConfig config = RocketRestConfig.builder(NASA_API_BASE_URL)
                .defaultOptions(options -> {
                    // Set logging options
                    options.set(RocketRestOptions.LOGGING_ENABLED, true);
                    options.set(RocketRestOptions.LOG_RESPONSE_BODY, true);
                })
                .build();
        
        RocketRest client = new RocketRest(NASA_API_BASE_URL, config);
        
        try {
            // Optional: Get specific date or use today
            LocalDate date = promptForDate();
            
            // Build query parameters
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("api_key", apiKey);
            
            // Add date parameter if user selected a specific date
            if (date != null) {
                String formattedDate = date.format(DateTimeFormatter.ISO_DATE);
                queryParams.put("date", formattedDate);
                System.out.println("\nFetching APOD for date: " + formattedDate);
            } else {
                System.out.println("\nFetching today's APOD");
            }
            
            // Display raw response data from API for debugging
            System.out.println("\nMaking request to: " + NASA_API_BASE_URL + "/planetary/apod with params: " + queryParams);
            
            // Using the fluent API to handle success/error cases elegantly
            Result<ApodResponse, ApiError> result = client.fluent()
                    .get("/planetary/apod", ApodResponse.class, queryParams);
            
            // Process the result
            result.ifSuccess(this::displayApodInfo)
                  .ifFailure(this::handleApiError);
                  
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
    
    /**
     * Prompts the user to enter their NASA API key or use the default DEMO_KEY
     */
    private String promptForApiKey() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEnter your NASA API key (or press Enter to use DEMO_KEY): ");
        String key = scanner.nextLine().trim();
        
        // Use default if empty
        if (key.isEmpty()) {
            System.out.println("Using default DEMO_KEY");
            return DEFAULT_API_KEY;
        }
        
        return key;
    }
    
    /**
     * Prompts the user to enter a specific date or use today's date
     */
    private LocalDate promptForDate() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEnter a date (YYYY-MM-DD) or press Enter for today's picture: ");
        String dateStr = scanner.nextLine().trim();
        
        // Return null if empty (will use today's date)
        if (dateStr.isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            System.out.println("Invalid date format. Using today's date instead.");
            return null;
        }
    }
    
    /**
     * Displays information about the Astronomy Picture of the Day
     */
    private void displayApodInfo(ApodResponse apod) {
        System.out.println("\n==== Astronomy Picture of the Day ====");
        System.out.println("Date: " + apod.getDate());
        System.out.println("Title: " + apod.getTitle());
        
        // Add null checks to avoid NullPointerException
        String mediaType = apod.getMediaType();
        System.out.println("Media Type: " + (mediaType != null ? mediaType : "unknown"));
        
        // Safe handling of media type
        if (mediaType != null) {
            if ("image".equals(mediaType)) {
                System.out.println("\nImage URL: " + apod.getUrl());
                if (apod.getHdurl() != null) {
                    System.out.println("HD Image URL: " + apod.getHdurl());
                }
            } else if ("video".equals(mediaType)) {
                System.out.println("\nVideo URL: " + apod.getUrl());
            } else {
                // Unknown media type, show URL anyway
                System.out.println("\nURL: " + apod.getUrl());
            }
        } else {
            // If media_type is missing, show URL anyway
            if (apod.getUrl() != null) {
                System.out.println("\nURL: " + apod.getUrl());
            }
        }
        
        if (apod.getExplanation() != null) {
            System.out.println("\nExplanation:");
            System.out.println(apod.getExplanation());
        }
        
        if (apod.getCopyright() != null && !apod.getCopyright().isEmpty()) {
            System.out.println("\nCopyright: " + apod.getCopyright());
        }
        
        // Debug - print all fields to help diagnose serialization issues
        System.out.println("\n--- Debug Info ---");
        System.out.println("Raw media_type value: " + apod.media_type);
        System.out.println("Raw url value: " + apod.url);
        System.out.println("Raw hdurl value: " + apod.hdurl);
    }
    
    /**
     * Handles API errors
     */
    private void handleApiError(ApiError error) {
        System.out.println("\n❌ API Error:");
        System.out.println("Status: " + error.getStatusCode());
        System.out.println("Message: " + error.getMessage());
        
        if (error.getStatusCode() == 403) {
            System.out.println("\nTip: You might be exceeding the rate limits of DEMO_KEY.");
            System.out.println("Get your free API key at: https://api.nasa.gov/");
        } else if (error.getStatusCode() == 429) {
            System.out.println("\nTip: You've hit the rate limit. Please try again later.");
        }
    }
    
    /**
     * POJO class representing the NASA APOD API response
     */
    public static class ApodResponse {
        // Use public fields for direct access in a debug section
        public String date;
        public String explanation;
        public String hdurl;
        public String media_type;
        public String service_version;
        public String title;
        public String url;
        public String copyright;
        
        // Required empty constructor for deserialization
        public ApodResponse() {
        }
        
        // Getters and setters
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public String getExplanation() {
            return explanation;
        }
        
        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
        
        public String getHdurl() {
            return hdurl;
        }
        
        public void setHdurl(String hdurl) {
            this.hdurl = hdurl;
        }
        
        public String getMediaType() {
            return media_type;  // This getter needs to return media_type field
        }
        
        public void setMediaType(String mediaType) {
            this.media_type = mediaType;
        }
        
        public String getServiceVersion() {
            return service_version;
        }
        
        public void setServiceVersion(String serviceVersion) {
            this.service_version = serviceVersion;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getCopyright() {
            return copyright;
        }
        
        public void setCopyright(String copyright) {
            this.copyright = copyright;
        }
        
        @Override
        public String toString() {
            return "ApodResponse{" +
                    "date='" + date + '\'' +
                    ", title='" + title + '\'' +
                    ", media_type='" + media_type + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
} 