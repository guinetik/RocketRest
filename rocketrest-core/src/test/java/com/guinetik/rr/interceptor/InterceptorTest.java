package com.guinetik.rr.interceptor;

import com.guinetik.rr.http.CircuitBreakerOpenException;
import com.guinetik.rr.http.RocketClient;
import com.guinetik.rr.http.RocketRestException;
import com.guinetik.rr.request.RequestBuilder;
import com.guinetik.rr.request.RequestSpec;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for the interceptor infrastructure including RequestInterceptor,
 * InterceptingClient, and RetryInterceptor.
 */
public class InterceptorTest {

    private MockRocketClient mockClient;
    private List<String> callOrder;

    @Before
    public void setUp() {
        mockClient = new MockRocketClient();
        callOrder = new ArrayList<String>();
    }

    // ==================== RequestInterceptor Tests ====================

    @Test
    public void testInterceptorDefaultMethods() {
        RequestInterceptor interceptor = new RequestInterceptor() {};

        RequestSpec<String, String> request = createRequest();

        // Default beforeRequest returns same request
        assertSame(request, interceptor.beforeRequest(request));

        // Default afterResponse returns same response
        String response = "response";
        assertSame(response, interceptor.afterResponse(response, request));

        // Default order is 0
        assertEquals(0, interceptor.getOrder());
    }

    @Test(expected = RocketRestException.class)
    public void testInterceptorDefaultOnErrorRethrows() throws RocketRestException {
        RequestInterceptor interceptor = new RequestInterceptor() {};

        RequestSpec<String, String> request = createRequest();
        RocketRestException exception = new RocketRestException("test error");
        InterceptorChain chain = createMockChain();

        interceptor.onError(exception, request, chain);
    }

    // ==================== InterceptingClient Tests ====================

    @Test
    public void testInterceptorOrderingBeforeRequest() {
        RequestInterceptor first = createOrderedInterceptor("first", -100);
        RequestInterceptor second = createOrderedInterceptor("second", 0);
        RequestInterceptor third = createOrderedInterceptor("third", 100);

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(third);  // Add out of order
        interceptors.add(first);
        interceptors.add(second);

        mockClient.setResponse("success");
        InterceptingClient client = new InterceptingClient(mockClient, interceptors);

        RequestSpec<String, String> request = createRequest();
        client.execute(request);

        // Verify order: first, second, third for beforeRequest
        assertTrue(callOrder.indexOf("first-before") < callOrder.indexOf("second-before"));
        assertTrue(callOrder.indexOf("second-before") < callOrder.indexOf("third-before"));
    }

    @Test
    public void testInterceptorOrderingAfterResponse() {
        RequestInterceptor first = createOrderedInterceptor("first", -100);
        RequestInterceptor second = createOrderedInterceptor("second", 0);
        RequestInterceptor third = createOrderedInterceptor("third", 100);

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(first);
        interceptors.add(second);
        interceptors.add(third);

        mockClient.setResponse("success");
        InterceptingClient client = new InterceptingClient(mockClient, interceptors);

        RequestSpec<String, String> request = createRequest();
        client.execute(request);

        // Verify reverse order: third, second, first for afterResponse
        assertTrue(callOrder.indexOf("third-after") < callOrder.indexOf("second-after"));
        assertTrue(callOrder.indexOf("second-after") < callOrder.indexOf("first-after"));
    }

    @Test
    public void testInterceptorModifiesRequest() {
        RequestInterceptor headerAdder = new RequestInterceptor() {
            @Override
            public <Req, Res> RequestSpec<Req, Res> beforeRequest(RequestSpec<Req, Res> request) {
                request.getHeaders().set("X-Custom-Header", "custom-value");
                return request;
            }
        };

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(headerAdder);

        mockClient.setResponse("success");
        InterceptingClient client = new InterceptingClient(mockClient, interceptors);

        RequestSpec<String, String> request = createRequest();
        client.execute(request);

        // Verify the mock client received the modified request
        assertEquals("custom-value", mockClient.lastRequest.getHeaders().get("X-Custom-Header"));
    }

    @Test
    public void testInterceptorModifiesResponse() {
        RequestInterceptor responseModifier = new RequestInterceptor() {
            @Override
            @SuppressWarnings("unchecked")
            public <Res> Res afterResponse(Res response, RequestSpec<?, Res> request) {
                if (response instanceof String) {
                    return (Res) (response + "-modified");
                }
                return response;
            }
        };

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(responseModifier);

        mockClient.setResponse("original");
        InterceptingClient client = new InterceptingClient(mockClient, interceptors);

        String result = client.execute(createRequest());
        assertEquals("original-modified", result);
    }

    @Test(expected = NullPointerException.class)
    public void testInterceptingClientRejectsNullDelegate() {
        new InterceptingClient(null, new ArrayList<RequestInterceptor>());
    }

    @Test
    public void testInterceptingClientAcceptsNullInterceptors() {
        mockClient.setResponse("success");
        InterceptingClient client = new InterceptingClient(mockClient, null);

        String result = client.execute(createRequest());
        assertEquals("success", result);
    }

    @Test
    public void testGetInterceptorsReturnsUnmodifiableList() {
        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(new RequestInterceptor() {});

        InterceptingClient client = new InterceptingClient(mockClient, interceptors);

        try {
            client.getInterceptors().add(new RequestInterceptor() {});
            fail("Should not be able to modify interceptor list");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    // ==================== RetryInterceptor Tests ====================

    @Test
    public void testRetryOnServerError() {
        AtomicInteger attempts = new AtomicInteger(0);
        mockClient = new MockRocketClient() {
            @Override
            public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
                lastRequest = requestSpec;
                if (attempts.incrementAndGet() < 3) {
                    throw new RocketRestException("Server error", 500, "Internal Server Error");
                }
                return (Res) response;
            }
        };
        mockClient.setResponse("success");

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(new RetryInterceptor(3, 10)); // 10ms delay for fast tests

        InterceptingClient client = new InterceptingClient(mockClient, interceptors, 5);

        String result = client.execute(createRequest());
        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    public void testRetryOnConnectionError() {
        AtomicInteger attempts = new AtomicInteger(0);
        mockClient = new MockRocketClient() {
            @Override
            public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
                lastRequest = requestSpec;
                if (attempts.incrementAndGet() < 2) {
                    throw new RocketRestException("Connection refused", 0, null);
                }
                return (Res) response;
            }
        };
        mockClient.setResponse("success");

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(new RetryInterceptor(3, 10));

        InterceptingClient client = new InterceptingClient(mockClient, interceptors, 5);

        String result = client.execute(createRequest());
        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    public void testNoRetryOn4xxClientError() {
        AtomicInteger attempts = new AtomicInteger(0);
        mockClient = new MockRocketClient() {
            @Override
            public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
                attempts.incrementAndGet();
                throw new RocketRestException("Bad Request", 400, "Invalid input");
            }
        };

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(new RetryInterceptor(3, 10));

        InterceptingClient client = new InterceptingClient(mockClient, interceptors, 5);

        try {
            client.execute(createRequest());
            fail("Should throw exception");
        } catch (RocketRestException e) {
            assertEquals(400, e.getStatusCode());
        }

        assertEquals(1, attempts.get()); // No retry
    }

    @Test
    public void testNoRetryOnCircuitBreakerOpen() {
        AtomicInteger attempts = new AtomicInteger(0);
        mockClient = new MockRocketClient() {
            @Override
            public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
                attempts.incrementAndGet();
                throw new CircuitBreakerOpenException("Circuit is open");
            }
        };

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(new RetryInterceptor(3, 10));

        InterceptingClient client = new InterceptingClient(mockClient, interceptors, 5);

        try {
            client.execute(createRequest());
            fail("Should throw exception");
        } catch (CircuitBreakerOpenException e) {
            // Expected
        }

        assertEquals(1, attempts.get()); // No retry
    }

    @Test
    public void testMaxRetriesExceeded() {
        AtomicInteger attempts = new AtomicInteger(0);
        mockClient = new MockRocketClient() {
            @Override
            public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
                attempts.incrementAndGet();
                throw new RocketRestException("Server error", 500, "Always fails");
            }
        };

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(new RetryInterceptor(2, 10));

        InterceptingClient client = new InterceptingClient(mockClient, interceptors, 5);

        try {
            client.execute(createRequest());
            fail("Should throw exception");
        } catch (RocketRestException e) {
            assertEquals(500, e.getStatusCode());
        }

        assertEquals(3, attempts.get()); // Initial + 2 retries
    }

    @Test
    public void testRetryOn429TooManyRequests() {
        AtomicInteger attempts = new AtomicInteger(0);
        mockClient = new MockRocketClient() {
            @Override
            public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
                lastRequest = requestSpec;
                if (attempts.incrementAndGet() < 2) {
                    throw new RocketRestException("Rate limited", 429, "Too Many Requests");
                }
                return (Res) response;
            }
        };
        mockClient.setResponse("success");

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(new RetryInterceptor(3, 10));

        InterceptingClient client = new InterceptingClient(mockClient, interceptors, 5);

        String result = client.execute(createRequest());
        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    public void testRetryOn408RequestTimeout() {
        AtomicInteger attempts = new AtomicInteger(0);
        mockClient = new MockRocketClient() {
            @Override
            public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
                lastRequest = requestSpec;
                if (attempts.incrementAndGet() < 2) {
                    throw new RocketRestException("Timeout", 408, "Request Timeout");
                }
                return (Res) response;
            }
        };
        mockClient.setResponse("success");

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(new RetryInterceptor(3, 10));

        InterceptingClient client = new InterceptingClient(mockClient, interceptors, 5);

        String result = client.execute(createRequest());
        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    public void testRetryInterceptorBuilder() {
        RetryInterceptor interceptor = RetryInterceptor.builder()
                .maxRetries(5)
                .initialDelayMs(100)
                .backoffMultiplier(1.5)
                .maxDelayMs(5000)
                .retryOn(e -> e.getStatusCode() == 503)
                .build();

        assertNotNull(interceptor);
        assertEquals(100, interceptor.getOrder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetryInterceptorRejectsNegativeRetries() {
        new RetryInterceptor(-1, 1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetryInterceptorRejectsNegativeDelay() {
        new RetryInterceptor(3, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetryInterceptorRejectsLowMultiplier() {
        new RetryInterceptor(3, 1000, 0.5);
    }

    // ==================== Error Recovery Tests ====================

    @Test
    public void testInterceptorCanRecoverFromError() {
        RequestInterceptor recoveryInterceptor = new RequestInterceptor() {
            @Override
            @SuppressWarnings("unchecked")
            public <Req, Res> Res onError(RocketRestException e, RequestSpec<Req, Res> request,
                                           InterceptorChain chain) throws RocketRestException {
                if (e.getStatusCode() == 404) {
                    return (Res) "fallback-response";
                }
                throw e;
            }
        };

        mockClient = new MockRocketClient() {
            @Override
            public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
                throw new RocketRestException("Not found", 404, "Resource not found");
            }
        };

        List<RequestInterceptor> interceptors = new ArrayList<RequestInterceptor>();
        interceptors.add(recoveryInterceptor);

        InterceptingClient client = new InterceptingClient(mockClient, interceptors);

        String result = client.execute(createRequest());
        assertEquals("fallback-response", result);
    }

    // ==================== Helper Methods ====================

    @SuppressWarnings("unchecked")
    private RequestSpec<String, String> createRequest() {
        return RequestBuilder.<String, String>get("/test")
                .responseType((Class<String>) (Class<?>) String.class)
                .build();
    }

    private InterceptorChain createMockChain() {
        return new InterceptorChain() {
            @Override
            public <Req, Res> Res retry(RequestSpec<Req, Res> request) throws RocketRestException {
                throw new RocketRestException("Mock retry");
            }

            @Override
            public int getRetryCount() {
                return 0;
            }

            @Override
            public int getMaxRetries() {
                return 3;
            }
        };
    }

    private RequestInterceptor createOrderedInterceptor(final String name, final int order) {
        return new RequestInterceptor() {
            @Override
            public <Req, Res> RequestSpec<Req, Res> beforeRequest(RequestSpec<Req, Res> request) {
                callOrder.add(name + "-before");
                return request;
            }

            @Override
            public <Res> Res afterResponse(Res response, RequestSpec<?, Res> request) {
                callOrder.add(name + "-after");
                return response;
            }

            @Override
            public int getOrder() {
                return order;
            }
        };
    }

    /**
     * Simple mock RocketClient for testing.
     */
    private static class MockRocketClient implements RocketClient {
        protected Object response;
        protected RequestSpec<?, ?> lastRequest;

        public void setResponse(Object response) {
            this.response = response;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
            lastRequest = requestSpec;
            return (Res) response;
        }

        @Override
        public void configureSsl(SSLContext sslContext) {
            // No-op
        }

        @Override
        public void setBaseUrl(String baseUrl) {
            // No-op
        }
    }
}
