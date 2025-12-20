package com.guinetik.rr.http;

import com.guinetik.rr.request.RequestSpec;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Unit tests for CircuitBreakerClient focusing on validation, thread-safety,
 * and the HALF_OPEN race condition fix.
 */
public class CircuitBreakerClientTest {

    /**
     * Simple mock RocketClient for testing that throws RocketRestException.
     */
    private static class TestRocketClient implements RocketClient {
        private Supplier<Object> responseSupplier;
        private final AtomicInteger callCount = new AtomicInteger(0);

        public void setResponse(Supplier<Object> supplier) {
            this.responseSupplier = supplier;
        }

        public void setSuccessResponse(Object response) {
            this.responseSupplier = () -> response;
        }

        public void setErrorResponse(int statusCode, String message) {
            this.responseSupplier = () -> {
                throw new RocketRestException(message, statusCode, message);
            };
        }

        public int getCallCount() {
            return callCount.get();
        }

        public void resetCallCount() {
            callCount.set(0);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
            callCount.incrementAndGet();
            if (responseSupplier != null) {
                return (Res) responseSupplier.get();
            }
            return null;
        }

        @Override
        public void configureSsl(SSLContext sslContext) {}

        @Override
        public void setBaseUrl(String baseUrl) {}
    }

    // ==================== Constructor Validation Tests ====================

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullDelegate() {
        new CircuitBreakerClient(null);
    }

    @Test(expected = NullPointerException.class)
    public void testFullConstructorRejectsNullDelegate() {
        new CircuitBreakerClient(null, 5, 30000, 60000,
                CircuitBreakerClient.FailurePolicy.ALL_EXCEPTIONS, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroFailureThreshold() {
        TestRocketClient mock = new TestRocketClient();
        new CircuitBreakerClient(mock, 0, 30000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeFailureThreshold() {
        TestRocketClient mock = new TestRocketClient();
        new CircuitBreakerClient(mock, -1, 30000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeResetTimeout() {
        TestRocketClient mock = new TestRocketClient();
        new CircuitBreakerClient(mock, 5, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeDecayTime() {
        TestRocketClient mock = new TestRocketClient();
        new CircuitBreakerClient(mock, 5, 30000, -1,
                CircuitBreakerClient.FailurePolicy.ALL_EXCEPTIONS, null);
    }

    @Test
    public void testConstructorAcceptsValidParameters() {
        TestRocketClient mock = new TestRocketClient();
        CircuitBreakerClient client = new CircuitBreakerClient(mock, 1, 0);
        assertEquals(CircuitBreakerClient.State.CLOSED, client.getState());
        assertEquals(0, client.getFailureCount());
    }

    @Test
    public void testConstructorDefaultsNullFailurePolicy() {
        TestRocketClient mock = new TestRocketClient();
        CircuitBreakerClient client = new CircuitBreakerClient(mock, 5, 30000, 60000,
                null, null);
        // Should not throw and should default to ALL_EXCEPTIONS behavior
        assertEquals(CircuitBreakerClient.State.CLOSED, client.getState());
    }

    // ==================== Basic Circuit Breaker Tests ====================

    @Test
    public void testCircuitStartsClosed() {
        TestRocketClient mock = new TestRocketClient();
        CircuitBreakerClient client = new CircuitBreakerClient(mock);
        assertEquals(CircuitBreakerClient.State.CLOSED, client.getState());
    }

    @Test
    public void testSuccessfulRequestKeepsCircuitClosed() {
        TestRocketClient mock = new TestRocketClient();
        mock.setSuccessResponse("success");

        CircuitBreakerClient client = new CircuitBreakerClient(mock, 3, 1000);

        RequestSpec<Void, String> request = createGetRequest("/test", String.class);
        String result = client.execute(request);

        assertEquals("success", result);
        assertEquals(CircuitBreakerClient.State.CLOSED, client.getState());
        assertEquals(0, client.getFailureCount());
    }

    @Test
    public void testCircuitOpensAfterThresholdFailures() {
        TestRocketClient mock = new TestRocketClient();
        mock.setErrorResponse(500, "Server error");

        CircuitBreakerClient client = new CircuitBreakerClient(mock, 3, 1000);
        RequestSpec<Void, String> request = createGetRequest("/error", String.class);

        // Make threshold failures
        for (int i = 0; i < 3; i++) {
            try {
                client.execute(request);
                fail("Should have thrown exception");
            } catch (RocketRestException e) {
                // Expected
            }
        }

        assertEquals(CircuitBreakerClient.State.OPEN, client.getState());
    }

    @Test
    public void testOpenCircuitRejectsRequests() {
        TestRocketClient mock = new TestRocketClient();
        mock.setErrorResponse(500, "Server error");

        CircuitBreakerClient client = new CircuitBreakerClient(mock, 2, 10000);
        RequestSpec<Void, String> request = createGetRequest("/error", String.class);

        // Trip the circuit
        for (int i = 0; i < 2; i++) {
            try {
                client.execute(request);
            } catch (RocketRestException e) {
                // Expected
            }
        }

        assertEquals(CircuitBreakerClient.State.OPEN, client.getState());

        // Now set success response
        mock.setSuccessResponse("success");
        mock.resetCallCount();

        // Try to make a request - should be rejected without calling the mock
        try {
            client.execute(request);
            fail("Should have thrown CircuitBreakerOpenException");
        } catch (CircuitBreakerOpenException e) {
            assertTrue(e.getResetTimeoutMs() > 0);
            assertEquals(0, mock.getCallCount()); // Delegate should not be called
        }
    }

    // ==================== HALF_OPEN Race Condition Tests ====================

    @Test
    public void testHalfOpenAllowsOnlyOneTestRequest() throws Exception {
        TestRocketClient mock = new TestRocketClient();

        // Very short reset timeout to quickly enter HALF_OPEN
        CircuitBreakerClient client = new CircuitBreakerClient(mock, 1, 50);

        // Force circuit to OPEN by causing a failure
        mock.setErrorResponse(500, "Server error");
        RequestSpec<Void, String> request = createGetRequest("/test", String.class);
        try {
            client.execute(request);
        } catch (RocketRestException e) {
            // Expected
        }

        assertEquals(CircuitBreakerClient.State.OPEN, client.getState());

        // Wait for reset timeout to allow HALF_OPEN
        Thread.sleep(100);

        // Set up slow response that will be used for testing
        mock.setResponse(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "success";
        });

        // Now try to make concurrent requests - only one should get through as test
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    client.execute(request);
                    successCount.incrementAndGet();
                } catch (CircuitBreakerOpenException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    // Other exceptions
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all to complete
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Only one request should have been allowed through as the test request
        // Others should have been rejected
        assertEquals("Only one test request should succeed", 1, successCount.get());
        assertEquals("Other requests should be rejected", 4, rejectedCount.get());
    }

    @Test
    public void testHalfOpenTestFlagResetOnSuccess() throws Exception {
        TestRocketClient mock = new TestRocketClient();
        CircuitBreakerClient client = new CircuitBreakerClient(mock, 1, 50);

        // Trip the circuit
        mock.setErrorResponse(500, "Server error");
        RequestSpec<Void, String> request = createGetRequest("/test", String.class);
        try {
            client.execute(request);
        } catch (RocketRestException e) {
            // Expected
        }

        Thread.sleep(100); // Wait for HALF_OPEN

        // Set success response
        mock.setSuccessResponse("success");

        // First request should succeed and close circuit
        String result = client.execute(request);
        assertEquals("success", result);
        assertEquals(CircuitBreakerClient.State.CLOSED, client.getState());

        // Verify halfOpenTestInProgress is false via metrics
        Map<String, Object> metrics = client.getMetrics();
        assertFalse((Boolean) metrics.get("halfOpenTestInProgress"));
    }

    @Test
    public void testHalfOpenTestFlagResetOnFailure() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        TestRocketClient mock = new TestRocketClient();
        mock.setResponse(() -> {
            if (callCount.incrementAndGet() <= 2) {
                throw new RocketRestException("Server error", 500, "error");
            }
            return "success";
        });

        CircuitBreakerClient client = new CircuitBreakerClient(mock, 1, 50);
        RequestSpec<Void, String> request = createGetRequest("/test", String.class);

        // First call trips the circuit
        try {
            client.execute(request);
        } catch (RocketRestException e) {
            // Expected
        }

        Thread.sleep(100); // Wait for HALF_OPEN

        // Second call should fail and keep circuit OPEN
        try {
            client.execute(request);
        } catch (RocketRestException e) {
            // Expected
        }

        assertEquals(CircuitBreakerClient.State.OPEN, client.getState());

        // Verify halfOpenTestInProgress is false via metrics
        Map<String, Object> metrics = client.getMetrics();
        assertFalse((Boolean) metrics.get("halfOpenTestInProgress"));
    }

    // ==================== Reset and Health Check Tests ====================

    @Test
    public void testManualResetClearsAllState() {
        TestRocketClient mock = new TestRocketClient();
        mock.setErrorResponse(500, "Server error");

        CircuitBreakerClient client = new CircuitBreakerClient(mock, 2, 30000);
        RequestSpec<Void, String> request = createGetRequest("/error", String.class);

        // Trip the circuit
        for (int i = 0; i < 2; i++) {
            try {
                client.execute(request);
            } catch (RocketRestException e) {
                // Expected
            }
        }

        assertEquals(CircuitBreakerClient.State.OPEN, client.getState());

        // Reset manually
        client.resetCircuit();

        assertEquals(CircuitBreakerClient.State.CLOSED, client.getState());
        assertEquals(0, client.getFailureCount());

        Map<String, Object> metrics = client.getMetrics();
        assertFalse((Boolean) metrics.get("halfOpenTestInProgress"));
    }

    @Test
    public void testMetricsIncludeHalfOpenTestStatus() {
        TestRocketClient mock = new TestRocketClient();
        CircuitBreakerClient client = new CircuitBreakerClient(mock);
        Map<String, Object> metrics = client.getMetrics();

        assertTrue(metrics.containsKey("halfOpenTestInProgress"));
        assertFalse((Boolean) metrics.get("halfOpenTestInProgress"));
    }

    // ==================== Helper Methods ====================

    private <T> RequestSpec<Void, T> createGetRequest(String endpoint, Class<T> responseType) {
        return new RequestSpec<>(
            endpoint,
            "GET",
            Collections.emptyMap(),
            RocketHeaders.defaultJson(),
            null,
            responseType
        );
    }
}
