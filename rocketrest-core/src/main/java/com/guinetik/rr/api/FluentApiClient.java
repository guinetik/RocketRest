package com.guinetik.rr.api;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.http.FluentHttpClient;
import com.guinetik.rr.http.RocketClientFactory;
import com.guinetik.rr.request.RequestBuilder;
import com.guinetik.rr.request.RequestSpec;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;

import java.util.Map;

/**
 * A fluent API client that uses the Result pattern instead of exceptions.
 * This provides a more declarative way to handle API calls.
 */
public class FluentApiClient extends AbstractApiClient {

    private final FluentHttpClient fluentClient;
    
    /**
     * Creates a new FluentApiClient with the specified base URL and configuration.
     *
     * @param baseUrl The base URL for API requests
     * @param config  The RocketRest configuration
     */
    public FluentApiClient(String baseUrl, RocketRestConfig config) {
        super(baseUrl, config, createFluentHttpClient(baseUrl, config));
        this.fluentClient = (FluentHttpClient) httpClient;
    }
    
    /**
     * Creates a new FluentApiClient with the specified base URL, configuration, and a
     * pre-configured FluentHttpClient instance.
     *
     * @param baseUrl      The base URL for API requests
     * @param config       The RocketRest configuration
     * @param fluentClient A pre-configured FluentHttpClient instance
     */
    public FluentApiClient(String baseUrl, RocketRestConfig config, FluentHttpClient fluentClient) {
        super(baseUrl, config, fluentClient);
        this.fluentClient = fluentClient;
    }
    
    /**
     * Executes a request and returns a Result object instead of throwing exceptions.
     *
     * @param <Req>       The type of the request
     * @param <Res>       The type of the response
     * @param requestSpec The request specification
     * @return A Result containing either the response or an ApiError
     */
    public <Req, Res> Result<Res, ApiError> executeWithResult(RequestSpec<Req, Res> requestSpec) {
        // Apply any request transformations from the abstract client
        refreshToken(); // Refresh token if needed
        logRequest(requestSpec); // Log the request if enabled
        
        // Delegate to the fluent client
        return fluentClient.executeWithResult(requestSpec);
    }
    
    /**
     * Performs a GET request using the Result pattern.
     *
     * @param <Res>          The type of the response
     * @param endpoint       The API endpoint
     * @param responseType   The class of the response type
     * @return A Result containing either the response or an ApiError
     */
    public <Res> Result<Res, ApiError> get(String endpoint, Class<Res> responseType) {
        return this.<Res>get(endpoint, null, responseType);
    }
    
    /**
     * Performs a GET request with headers using the Result pattern.
     *
     * @param <Res>          The type of the response
     * @param endpoint       The API endpoint
     * @param headers        The request headers
     * @param responseType   The class of the response type
     * @return A Result containing either the response or an ApiError
     */
    public <Res> Result<Res, ApiError> get(String endpoint, Map<String, String> headers, Class<Res> responseType) {
        RequestSpec<Void, Res> request = RequestBuilder.<Void, Res>get(endpoint)
                .headers(createHeaders(headers))
                .responseType(responseType)
                .build();
                
        return this.<Void, Res>executeWithResult(request);
    }
    
    /**
     * Performs a POST request using the Result pattern.
     *
     * @param <Req>          The type of the request body
     * @param <Res>          The type of the response
     * @param endpoint       The API endpoint
     * @param body           The request body
     * @param responseType   The class of the response type
     * @return A Result containing either the response or an ApiError
     */
    public <Req, Res> Result<Res, ApiError> post(String endpoint, Req body, Class<Res> responseType) {
        return this.<Req, Res>post(endpoint, body, null, responseType);
    }
    
    /**
     * Performs a POST request with headers using the Result pattern.
     *
     * @param <Req>          The type of the request body
     * @param <Res>          The type of the response
     * @param endpoint       The API endpoint
     * @param body           The request body
     * @param headers        The request headers
     * @param responseType   The class of the response type
     * @return A Result containing either the response or an ApiError
     */
    public <Req, Res> Result<Res, ApiError> post(String endpoint, Req body, Map<String, String> headers, Class<Res> responseType) {
        RequestSpec<Req, Res> request = RequestBuilder.<Req, Res>post(endpoint)
                .headers(createHeaders(headers))
                .body(body)
                .responseType(responseType)
                .build();
                
        return this.<Req, Res>executeWithResult(request);
    }
    
    /**
     * Performs a PUT request using the Result pattern.
     *
     * @param <Req>          The type of the request body
     * @param <Res>          The type of the response
     * @param endpoint       The API endpoint
     * @param body           The request body
     * @param responseType   The class of the response type
     * @return A Result containing either the response or an ApiError
     */
    public <Req, Res> Result<Res, ApiError> put(String endpoint, Req body, Class<Res> responseType) {
        return this.<Req, Res>put(endpoint, body, null, responseType);
    }
    
    /**
     * Performs a PUT request with headers using the Result pattern.
     *
     * @param <Req>          The type of the request body
     * @param <Res>          The type of the response
     * @param endpoint       The API endpoint
     * @param body           The request body
     * @param headers        The request headers
     * @param responseType   The class of the response type
     * @return A Result containing either the response or an ApiError
     */
    public <Req, Res> Result<Res, ApiError> put(String endpoint, Req body, Map<String, String> headers, Class<Res> responseType) {
        RequestSpec<Req, Res> request = RequestBuilder.<Req, Res>put(endpoint)
                .headers(createHeaders(headers))
                .body(body)
                .responseType(responseType)
                .build();
                
        return this.<Req, Res>executeWithResult(request);
    }
    
    /**
     * Performs a DELETE request using the Result pattern.
     *
     * @param <Res>          The type of the response
     * @param endpoint       The API endpoint
     * @param responseType   The class of the response type
     * @return A Result containing either the response or an ApiError
     */
    public <Res> Result<Res, ApiError> delete(String endpoint, Class<Res> responseType) {
        return this.<Res>delete(endpoint, null, responseType);
    }
    
    /**
     * Performs a DELETE request with headers using the Result pattern.
     *
     * @param <Res>          The type of the response
     * @param endpoint       The API endpoint
     * @param headers        The request headers
     * @param responseType   The class of the response type
     * @return A Result containing either the response or an ApiError
     */
    public <Res> Result<Res, ApiError> delete(String endpoint, Map<String, String> headers, Class<Res> responseType) {
        RequestSpec<Void, Res> request = RequestBuilder.<Void, Res>delete(endpoint)
                .headers(createHeaders(headers))
                .responseType(responseType)
                .build();
                
        return this.<Void, Res>executeWithResult(request);
    }
    
    /**
     * Shutdown the client and release resources.
     */
    public void shutdown() {
        // Currently just a placeholder for API compatibility with AsyncApiClient
        // FluentHttpClient doesn't require explicit shutdown
    }
    
    /**
     * Creates a FluentHttpClient with the appropriate options from config.
     *
     * @param baseUrl The base URL for API requests
     * @param config  The RocketRest configuration
     * @return A new FluentHttpClient
     */
    private static FluentHttpClient createFluentHttpClient(String baseUrl, RocketRestConfig config) {
        return RocketClientFactory.fromConfig(config)
                .buildFluent();
    }
} 