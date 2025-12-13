package com.guinetik.rr.request;

import com.guinetik.rr.http.RocketHeaders;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RequestBuilder} and {@link RequestSpec}.
 */
public class RequestBuilderTest {

    @Test
    public void testBuildGetRequest() {
        RequestSpec<Void, String> spec = new RequestBuilder<Void, String>()
                .endpoint("/users/1")
                .method("GET")
                .responseType(String.class)
                .build();

        assertEquals("/users/1", spec.getEndpoint());
        assertEquals("GET", spec.getMethod());
        assertNull(spec.getBody());
        assertEquals(String.class, spec.getResponseType());
    }

    @Test
    public void testBuildPostRequestWithBody() {
        String requestBody = "{\"name\":\"John\"}";
        RequestSpec<String, String> spec = new RequestBuilder<String, String>()
                .endpoint("/users")
                .method("POST")
                .body(requestBody)
                .responseType(String.class)
                .build();

        assertEquals("/users", spec.getEndpoint());
        assertEquals("POST", spec.getMethod());
        assertEquals(requestBody, spec.getBody());
        assertEquals(String.class, spec.getResponseType());
    }

    @Test
    public void testBuildRequestWithQueryParams() {
        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "10");

        RequestSpec<Void, String> spec = new RequestBuilder<Void, String>()
                .endpoint("/users")
                .method("GET")
                .queryParams(params)
                .responseType(String.class)
                .build();

        assertEquals("/users", spec.getEndpoint());
        assertNotNull(spec.getQueryParams());
        assertEquals("1", spec.getQueryParams().get("page"));
        assertEquals("10", spec.getQueryParams().get("limit"));
    }

    @Test
    public void testBuildRequestWithHeaders() {
        RocketHeaders headers = new RocketHeaders()
                .set("X-Custom-Header", "custom-value")
                .contentType(RocketHeaders.ContentTypes.APPLICATION_JSON);

        RequestSpec<Void, String> spec = new RequestBuilder<Void, String>()
                .endpoint("/api")
                .method("GET")
                .headers(headers)
                .responseType(String.class)
                .build();

        assertNotNull(spec.getHeaders());
        assertEquals("custom-value", spec.getHeaders().get("X-Custom-Header"));
        assertEquals(RocketHeaders.ContentTypes.APPLICATION_JSON, spec.getHeaders().get("Content-Type"));
    }

    @Test
    public void testBuildRequestWithSingleQueryParam() {
        RequestSpec<Void, String> spec = new RequestBuilder<Void, String>()
                .endpoint("/search")
                .method("GET")
                .queryParam("q", "test query")
                .responseType(String.class)
                .build();

        assertNotNull(spec.getQueryParams());
        assertEquals("test query", spec.getQueryParams().get("q"));
    }

    @Test
    public void testBuildPutRequest() {
        String body = "{\"id\":1,\"name\":\"Updated\"}";
        RequestSpec<String, String> spec = new RequestBuilder<String, String>()
                .endpoint("/users/1")
                .method("PUT")
                .body(body)
                .responseType(String.class)
                .build();

        assertEquals("PUT", spec.getMethod());
        assertEquals(body, spec.getBody());
    }

    @Test
    public void testBuildDeleteRequest() {
        RequestSpec<Void, String> spec = new RequestBuilder<Void, String>()
                .endpoint("/users/1")
                .method("DELETE")
                .responseType(String.class)
                .build();

        assertEquals("DELETE", spec.getMethod());
        assertNull(spec.getBody());
    }

    @Test
    public void testChainedBuilderMethods() {
        Map<String, String> params = new HashMap<>();
        params.put("active", "true");

        RocketHeaders headers = new RocketHeaders().bearerAuth("token123");

        RequestSpec<String, String> spec = new RequestBuilder<String, String>()
                .endpoint("/api/data")
                .method("POST")
                .body("{}")
                .queryParams(params)
                .headers(headers)
                .responseType(String.class)
                .build();

        assertEquals("/api/data", spec.getEndpoint());
        assertEquals("POST", spec.getMethod());
        assertEquals("{}", spec.getBody());
        assertEquals("true", spec.getQueryParams().get("active"));
        assertTrue(spec.getHeaders().get("Authorization").startsWith("Bearer "));
    }

    @Test
    public void testEmptyQueryParams() {
        RequestSpec<Void, String> spec = new RequestBuilder<Void, String>()
                .endpoint("/api")
                .method("GET")
                .responseType(String.class)
                .build();

        // Query params should be empty or null for request without params
        assertTrue(spec.getQueryParams() == null || spec.getQueryParams().isEmpty());
    }
}
