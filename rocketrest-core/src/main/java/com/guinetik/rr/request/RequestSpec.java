package com.guinetik.rr.request;

import com.guinetik.rr.http.RocketHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable specification of an HTTP request containing all parameters needed for execution.
 *
 * <p>A {@code RequestSpec} encapsulates the complete definition of an API request including
 * the endpoint, HTTP method, query parameters, headers, request body, and expected response type.
 * Instances are created using the {@link RequestBuilder} fluent API.
 *
 * <h2>Request Components</h2>
 * <ul>
 *   <li><b>Endpoint</b> - The API path (relative or absolute URL)</li>
 *   <li><b>Method</b> - HTTP method (GET, POST, PUT, DELETE, etc.)</li>
 *   <li><b>Query Parameters</b> - URL query string parameters</li>
 *   <li><b>Headers</b> - HTTP request headers via {@link RocketHeaders}</li>
 *   <li><b>Body</b> - Request payload (for POST, PUT, PATCH)</li>
 *   <li><b>Response Type</b> - Expected Java class for response deserialization</li>
 * </ul>
 *
 * <h2>Creating Request Specifications</h2>
 * <pre class="language-java"><code>
 * // Simple GET request
 * RequestSpec&lt;Void, User&gt; getUser = RequestBuilder.&lt;Void, User&gt;get("/users/1")
 *     .responseType(User.class)
 *     .build();
 *
 * // POST request with body
 * CreateUserRequest body = new CreateUserRequest("John", "john@example.com");
 * RequestSpec&lt;CreateUserRequest, User&gt; createUser = RequestBuilder
 *     .&lt;CreateUserRequest, User&gt;post("/users")
 *     .body(body)
 *     .responseType(User.class)
 *     .build();
 *
 * // GET with query parameters
 * Map&lt;String, String&gt; params = new HashMap&lt;&gt;();
 * params.put("page", "1");
 * params.put("limit", "20");
 * RequestSpec&lt;Void, UserList&gt; listUsers = RequestBuilder.&lt;Void, UserList&gt;get("/users")
 *     .queryParams(params)
 *     .responseType(UserList.class)
 *     .build();
 * </code></pre>
 *
 * <h2>Executing Requests</h2>
 * <pre class="language-java"><code>
 * // With synchronous client
 * User user = client.sync().execute(getUser);
 *
 * // With async client
 * CompletableFuture&lt;User&gt; future = client.async().execute(getUser);
 *
 * // With fluent client
 * Result&lt;User, ApiError&gt; result = client.fluent().execute(getUser);
 * </code></pre>
 *
 * @param <Req> the type of the request body (use {@code Void} for requests without body)
 * @param <Res> the type of the expected response
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RequestBuilder
 * @see RocketHeaders
 * @since 1.0.0
 */
public class RequestSpec<Req, Res> {
    private final String endpoint;
    private final String method;
    private final Map<String, String> queryParams;
    private final RocketHeaders headers;
    private final Req body;
    private final Class<Res> responseType;

    public RequestSpec(String endpoint, String method, Map<String, String> queryParams,
                       RocketHeaders headers, Req body, Class<Res> responseType) {
        this.endpoint = endpoint;
        this.method = method;
        this.queryParams = queryParams != null ? queryParams : new HashMap<>();
        this.headers = headers != null ? headers : RocketHeaders.defaultJson();
        this.body = body;
        this.responseType = responseType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public RocketHeaders getHeaders() {
        return headers;
    }

    public Req getBody() {
        return body;
    }

    public Class<Res> getResponseType() {
        return responseType;
    }
}
