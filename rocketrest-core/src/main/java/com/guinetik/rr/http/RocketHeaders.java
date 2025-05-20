package com.guinetik.rr.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents HTTP headers for requests in a structured way.
 * Provides convenience methods for common headers.
 */
public class RocketHeaders {
    
    private final Map<String, String> headers;
    
    /**
     * Standard HTTP header names as constants.
     */
    public static final class Names {
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String ACCEPT = "Accept";
        public static final String AUTHORIZATION = "Authorization";
        public static final String USER_AGENT = "User-Agent";
        public static final String CONTENT_LENGTH = "Content-Length";
    }
    
    /**
     * Common content types as constants.
     */
    public static final class ContentTypes {
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_FORM = "application/x-www-form-urlencoded";
        public static final String TEXT_PLAIN = "text/plain";
        public static final String MULTIPART_FORM = "multipart/form-data";
    }
    
    /**
     * Creates a new empty header container.
     */
    public RocketHeaders() {
        this.headers = new HashMap<>();
    }
    
    /**
     * Creates a new header container with the specified headers.
     *
     * @param headers The initial headers
     */
    public RocketHeaders(Map<String, String> headers) {
        this.headers = new HashMap<>(headers);
    }
    
    /**
     * Sets a header value.
     *
     * @param name The header name
     * @param value The header value
     * @return This HttpHeader instance for chaining
     */
    public RocketHeaders set(String name, String value) {
        headers.put(name, value);
        return this;
    }
    
    /**
     * Gets a header value.
     *
     * @param name The header name
     * @return The header value or null if not present
     */
    public String get(String name) {
        return headers.get(name);
    }
    
    /**
     * Removes a header.
     *
     * @param name The header name
     * @return This HttpHeader instance for chaining
     */
    public RocketHeaders remove(String name) {
        headers.remove(name);
        return this;
    }
    
    /**
     * Checks if a header is present.
     *
     * @param name The header name
     * @return true if the header is present
     */
    public boolean contains(String name) {
        return headers.containsKey(name);
    }
    
    /**
     * Sets the Content-Type header.
     *
     * @param contentType The content type
     * @return This HttpHeader instance for chaining
     */
    public RocketHeaders contentType(String contentType) {
        return set(Names.CONTENT_TYPE, contentType);
    }
    
    /**
     * Sets the Accept header.
     *
     * @param accept The accept value
     * @return This HttpHeader instance for chaining
     */
    public RocketHeaders accept(String accept) {
        return set(Names.ACCEPT, accept);
    }
    
    /**
     * Sets the Authorization header with a Bearer token.
     *
     * @param token The token
     * @return This HttpHeader instance for chaining
     */
    public RocketHeaders bearerAuth(String token) {
        return set(Names.AUTHORIZATION, "Bearer " + token);
    }
    
    /**
     * Sets the Authorization header with Basic authentication.
     *
     * @param username The username
     * @param password The password
     * @return This HttpHeader instance for chaining
     */
    public RocketHeaders basicAuth(String username, String password) {
        String auth = java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        return set(Names.AUTHORIZATION, "Basic " + auth);
    }
    
    /**
     * Gets all headers as a Map.
     *
     * @return An unmodifiable view of the header map
     */
    public Map<String, String> asMap() {
        return java.util.Collections.unmodifiableMap(headers);
    }
    
    /**
     * Merges this header with another, with values from the other taking precedence.
     *
     * @param other The other HttpHeader instance
     * @return A new HttpHeader instance with merged values
     */
    public RocketHeaders merge(RocketHeaders other) {
        RocketHeaders result = new RocketHeaders(this.headers);
        result.headers.putAll(other.headers);
        return result;
    }
    
    /**
     * Creates a set of default headers with JSON content type.
     *
     * @return A new HttpHeader with JSON content type and accept headers
     */
    public static RocketHeaders defaultJson() {
        return new RocketHeaders()
            .contentType(ContentTypes.APPLICATION_JSON)
            .accept(ContentTypes.APPLICATION_JSON);
    }
} 