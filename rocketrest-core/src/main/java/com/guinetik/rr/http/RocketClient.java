package com.guinetik.rr.http;

import com.guinetik.rr.request.RequestSpec;

/**
 * Interface for HTTP request clients. This abstraction allows for different
 * HTTP client implementations while maintaining a consistent API.
 */
public interface RocketClient {
    
    /**
     * Executes an HTTP request based on the provided request specification.
     *
     * @param <Req>       The type of the request body.
     * @param <Res>       The type of the response.
     * @param requestSpec The specification of the request to be executed.
     * @return The response object.
     * @throws RocketRestException If an error occurs during the request execution.
     */
    <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException;
    
    /**
     * Sets the SSL context to be used for HTTPS requests.
     * 
     * @param sslContext The SSL context to use.
     */
    void configureSsl(javax.net.ssl.SSLContext sslContext);

    void setBaseUrl(String baseUrl);
} 