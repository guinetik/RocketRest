package com.guinetik.rr.http;

import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.request.RequestSpec;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;

/**
 * A fluent HTTP client implementation that uses the Result pattern instead of exceptions.
 * This client offers a more declarative way to handle errors without exceptions by delegating
 * to an underlying RocketClient implementation.
 */
public class FluentHttpClient implements RocketClient {

    private static final Logger logger = LoggerFactory.getLogger(FluentHttpClient.class);

    private final RocketClient delegate;
    private String baseUrl;
    private final RocketRestOptions clientOptions;

    /**
     * Creates a new FluentHttpClient with the specified base URL.
     *
     * @param baseUrl The base URL for all requests
     */
    public FluentHttpClient(String baseUrl) {
        this(baseUrl, new RocketRestOptions());
    }

    /**
     * Creates a new FluentHttpClient with the specified base URL and client options.
     *
     * @param baseUrl       The base URL for all requests
     * @param clientOptions The client options
     */
    public FluentHttpClient(String baseUrl, RocketRestOptions clientOptions) {
        this.baseUrl = baseUrl;
        this.clientOptions = clientOptions != null ? clientOptions : new RocketRestOptions();
        this.delegate = new DefaultHttpClient(baseUrl, this.clientOptions);
    }

    /**
     * Creates a new FluentHttpClient that delegates to the specified RocketClient.
     *
     * @param delegate The RocketClient to delegate requests to
     * @param baseUrl The base URL for all requests
     * @param clientOptions The client options
     */
    public FluentHttpClient(RocketClient delegate, String baseUrl, RocketRestOptions clientOptions) {
        this.delegate = delegate;
        this.baseUrl = baseUrl;
        this.clientOptions = clientOptions != null ? clientOptions : new RocketRestOptions();
    }

    @Override
    public void configureSsl(SSLContext sslContext) {
        delegate.configureSsl(sslContext);
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Gets the client options.
     *
     * @return The client options
     */
    public RocketRestOptions getClientOptions() {
        return clientOptions;
    }

    /**
     * Gets the base URL.
     *
     * @return The base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Executes a request and returns a Result object containing either the response or an error.
     * This method is the primary API for executing requests in a functional way without exceptions.
     *
     * @param <Req>       The type of the request body
     * @param <Res>       The type of the response
     * @param requestSpec The request specification
     * @return A Result object containing either the response or an error
     */
    public <Req, Res> Result<Res, ApiError> executeWithResult(RequestSpec<Req, Res> requestSpec) {
        try {
            // Validate absolute URLs
            if (isAbsoluteUrl(requestSpec.getEndpoint()) && 
                !baseUrl.trim().isEmpty() && 
                !baseUrl.equals("/")) {
                return Result.failure(ApiError.configError(
                    "Cannot use absolute URL '" + requestSpec.getEndpoint() + "' with base URL '" + baseUrl + 
                    "'. Either use a relative path or set base URL to empty string."
                ));
            }
            
            // Delegate to the underlying client to execute the request
            Res response = delegate.execute(requestSpec);
            return Result.success(response);
        } catch (RocketRestException e) {
            // Convert exception to appropriate ApiError
            ApiError error = convertExceptionToApiError(e);
            return Result.failure(error);
        } catch (Exception e) {
            // Handle unexpected exceptions
            return Result.failure(ApiError.networkError("Unexpected error: " + e.getMessage()));
        }
    }

    @Override
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
        // Bridge to the exception-based API
        Result<Res, ApiError> result = executeWithResult(requestSpec);
        if (result.isSuccess()) {
            return result.getValue();
        } else {
            ApiError error = result.getError();
            throw new RocketRestException(
                    error.getMessage(),
                    error.getStatusCode(),
                    error.getResponseBody()
            );
        }
    }
    
    /**
     * Checks if a URL is absolute.
     *
     * @param url The URL to check
     * @return true if the URL is absolute, false otherwise
     */
    private boolean isAbsoluteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
    
    /**
     * Converts a RocketRestException to an appropriate ApiError.
     *
     * @param e The exception to convert
     * @return An ApiError representing the exception
     */
    private ApiError convertExceptionToApiError(RocketRestException e) {
        // Special handling for CircuitBreakerOpenException
        if (e instanceof CircuitBreakerOpenException) {
            CircuitBreakerOpenException cbException = (CircuitBreakerOpenException) e;
            return ApiError.circuitOpenError(e.getMessage());
        }
        
        int statusCode = e.getStatusCode();
        String body = e.getResponseBody();
        
        // Determine the error type based on the status code
        if (statusCode == HttpConstants.StatusCodes.UNAUTHORIZED) {
            return ApiError.authError(e.getMessage(), statusCode, body);
        } else if (statusCode >= HttpConstants.StatusCodes.CLIENT_ERROR_MIN && 
                   statusCode < HttpConstants.StatusCodes.SERVER_ERROR_MIN) {
            return ApiError.httpError(e.getMessage(), statusCode, body);
        } else if (statusCode >= HttpConstants.StatusCodes.SERVER_ERROR_MIN) {
            return ApiError.httpError(e.getMessage(), statusCode, body);
        } else if (e.getCause() instanceof java.io.IOException) {
            return ApiError.networkError(e.getMessage());
        } else {
            // Default to unknown error
            return ApiError.httpError(e.getMessage(), statusCode, body);
        }
    }
} 