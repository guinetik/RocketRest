package com.guinetik.rr.request;

import com.guinetik.rr.http.RocketHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the specification of an API request, including endpoint, method,
 * query parameters, headers, body, and response type.
 *
 * @param <Req>> The type of the request body
 * @param <Res>> The type of the response
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
