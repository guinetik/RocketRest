package com.guinetik.rr.request;

import com.guinetik.rr.http.RocketHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for constructing {@link RequestSpec} instances.
 *
 * @param <Req> The type of the request body.
 * @param <Res> The type of the response.
 */
public class RequestBuilder<Req, Res> {
    private String endpoint;
    private String method = "GET";
    private Map<String, String> queryParams = new HashMap<>();
    private RocketHeaders headers = RocketHeaders.defaultJson();
    private Req body;
    private Class<Res> responseType;

    /**
     * Creates a GET request builder for the specified endpoint.
     *
     * @param endpoint the API endpoint.
     * @param <Req> The type of the request body.
     * @param <Res> The type of the response.
     * @return a new builder instance.
     */
    public static <Req, Res> RequestBuilder<Req, Res> get(String endpoint) {
        return new RequestBuilder<Req, Res>().endpoint(endpoint).method("GET");
    }

    /**
     * Creates a POST request builder for the specified endpoint.
     *
     * @param endpoint the API endpoint.
     * @param <Req> The type of the request body.
     * @param <Res> The type of the response.
     * @return a new builder instance.
     */
    public static <Req, Res> RequestBuilder<Req, Res> post(String endpoint) {
        return new RequestBuilder<Req, Res>().endpoint(endpoint).method("POST");
    }

    /**
     * Creates a PUT request builder for the specified endpoint.
     *
     * @param endpoint the API endpoint.
     * @param <Req> The type of the request body.
     * @param <Res> The type of the response.
     * @return a new builder instance.
     */
    public static <Req, Res> RequestBuilder<Req, Res> put(String endpoint) {
        return new RequestBuilder<Req, Res>().endpoint(endpoint).method("PUT");
    }

    /**
     * Creates a DELETE request builder for the specified endpoint.
     *
     * @param endpoint the API endpoint.
     * @param <Req> The type of the request body.
     * @param <Res> The type of the response.
     * @return a new builder instance.
     */
    public static <Req, Res> RequestBuilder<Req, Res> delete(String endpoint) {
        return new RequestBuilder<Req, Res>().endpoint(endpoint).method("DELETE");
    }

    /**
     * Sets the API endpoint for the request.
     *
     * @param endpoint the API endpoint.
     * @return the builder instance.
     */
    public RequestBuilder<Req, Res> endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Sets the HTTP method for the request.
     *
     * @param method the HTTP method (e.g., GET, POST, PUT, DELETE).
     * @return the builder instance.
     */
    public RequestBuilder<Req, Res> method(String method) {
        this.method = method;
        return this;
    }

    /**
     * Sets the query parameters for the request.
     *
     * @param queryParams a map of query parameters.
     * @return the builder instance.
     */
    public RequestBuilder<Req, Res> queryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    /**
     * Sets the headers for the request.
     *
     * @param headers a map of headers.
     * @return the builder instance.
     */
    public RequestBuilder<Req, Res> headers(RocketHeaders headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Sets the body of the request.
     *
     * @param body the request body.
     * @return the builder instance.
     */
    public RequestBuilder<Req, Res> body(Req body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the expected response type of the request.
     *
     * @param responseType the response type.
     * @return the builder instance.
     */
    public RequestBuilder<Req, Res> responseType(Class<Res> responseType) {
        this.responseType = responseType;
        return this;
    }

    /**
     * Builds and returns a {@link RequestSpec} instance.
     *
     * @return the constructed {@link RequestSpec}.
     */
    public RequestSpec<Req, Res> build() {
        return new RequestSpec<>(endpoint, method, queryParams, headers, body, responseType);
    }
}
