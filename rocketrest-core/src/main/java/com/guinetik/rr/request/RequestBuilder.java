package com.guinetik.rr.request;

import com.guinetik.rr.http.RocketHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder for constructing {@link RequestSpec} instances.
 *
 * <p>This builder provides a clean, type-safe API for creating HTTP request specifications
 * with all necessary parameters like endpoint, method, headers, body, and response type.
 *
 * <h2>Static Factory Methods</h2>
 * <pre class="language-java"><code>
 * // GET request
 * RequestSpec&lt;Void, User&gt; getUser = RequestBuilder.&lt;Void, User&gt;get("/users/1")
 *     .responseType(User.class)
 *     .build();
 *
 * // POST request with body
 * RequestSpec&lt;CreateUser, User&gt; createUser = RequestBuilder.&lt;CreateUser, User&gt;post("/users")
 *     .body(new CreateUser("John", "john@example.com"))
 *     .responseType(User.class)
 *     .build();
 *
 * // PUT request
 * RequestSpec&lt;UpdateUser, User&gt; updateUser = RequestBuilder.&lt;UpdateUser, User&gt;put("/users/1")
 *     .body(new UpdateUser("John Doe"))
 *     .responseType(User.class)
 *     .build();
 *
 * // DELETE request
 * RequestSpec&lt;Void, Void&gt; deleteUser = RequestBuilder.&lt;Void, Void&gt;delete("/users/1")
 *     .responseType(Void.class)
 *     .build();
 * </code></pre>
 *
 * <h2>With Headers and Query Params</h2>
 * <pre class="language-java"><code>
 * Map&lt;String, String&gt; params = new HashMap&lt;&gt;();
 * params.put("page", "1");
 * params.put("limit", "10");
 *
 * RequestSpec&lt;Void, UserList&gt; request = RequestBuilder.&lt;Void, UserList&gt;get("/users")
 *     .queryParams(params)
 *     .headers(RocketHeaders.defaultJson().set("X-Custom", "value"))
 *     .responseType(UserList.class)
 *     .build();
 * </code></pre>
 *
 * @param <Req> the type of the request body
 * @param <Res> the type of the response
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RequestSpec
 * @since 1.0.0
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
