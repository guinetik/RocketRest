package com.guinetik.rr.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guinetik.rr.api.ApiException;
import com.guinetik.rr.request.RequestSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * Mock implementation of RocketClient for testing purposes.
 * Provides a way to simulate HTTP client behavior without making actual network requests.
 * Can be used with RocketClientFactory to provide mocked clients throughout the application.
 */
public class MockRocketClient implements RocketClient {
    private static final Logger logger = LoggerFactory.getLogger(MockRocketClient.class);

    /**
     * Represents a mock response rule with matching criteria
     */
    private static class MockRule {
        private final String method;
        private final Pattern urlPattern;
        private final BiFunction<String, Object, Object> responseProducer;

        public MockRule(String method, String urlPattern, BiFunction<String, Object, Object> responseProducer) {
            this.method = method;
            this.urlPattern = Pattern.compile(urlPattern);
            this.responseProducer = responseProducer;
        }

        public boolean matches(String method, String url) {
            return this.method.equalsIgnoreCase(method) && urlPattern.matcher(url).matches();
        }

        public Object produceResponse(String url, Object body) {
            return responseProducer.apply(url, body);
        }
    }
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Integer> invocationCounts = new HashMap<>();
    private final Map<String, Long> latencies = new HashMap<>();
    private final Map<String, Integer> statusCodes = new HashMap<>();
    // Store rules instead of a simple map to support pattern matching
    private final java.util.List<MockRule> mockRules = new java.util.ArrayList<>();
    private SSLContext sslContext;

    /**
     * Creates a new mock client instance.
     */
    public MockRocketClient() {
    }

    /**
     * Sets a custom header value that will be included in response data
     *
     * @param name  Header name
     * @param value Header value
     * @return This MockRocketClient instance for chaining
     */
    public MockRocketClient withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Sets the latency for a specific endpoint in milliseconds
     *
     * @param urlPattern URL pattern to match
     * @param latencyMs  Latency in milliseconds
     * @return This MockRocketClient instance for chaining
     */
    public MockRocketClient withLatency(String urlPattern, long latencyMs) {
        latencies.put(urlPattern, latencyMs);
        return this;
    }

    /**
     * Sets the status code for a specific endpoint
     *
     * @param urlPattern URL pattern to match
     * @param statusCode HTTP status code
     * @return This MockRocketClient instance for chaining
     */
    public MockRocketClient withStatusCode(String urlPattern, int statusCode) {
        statusCodes.put(urlPattern, statusCode);
        return this;
    }

    /**
     * Adds a mock response for a specific HTTP method and URL pattern.
     * The URL pattern is treated as a regex pattern for more flexible matching.
     *
     * @param method           HTTP method (GET, POST, PUT, DELETE)
     * @param urlPattern       URL pattern to match (regex supported)
     * @param responseProducer Function that takes (url, requestBody) and returns a response object
     */
    public MockRocketClient addMockResponse(String method, String urlPattern,
                                            BiFunction<String, Object, Object> responseProducer) {
        mockRules.add(new MockRule(method, urlPattern, responseProducer));
        return this;
    }

    /**
     * Gets the number of times a specific endpoint has been invoked
     *
     * @param method     HTTP method
     * @param urlPattern URL pattern
     * @return Number of invocations
     */
    public int getInvocationCount(String method, String urlPattern) {
        return invocationCounts.getOrDefault(method + ":" + urlPattern, 0);
    }

    /**
     * Resets all invocation counts
     */
    public void resetCounts() {
        invocationCounts.clear();
    }

    /**
     * Finds a matching mock response rule for the given method and URL.
     */
    private Optional<MockRule> findMatchingRule(String method, String url) {
        return mockRules.stream()
                .filter(rule -> rule.matches(method, url))
                .findFirst();
    }

    @Override
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
        String method = requestSpec.getMethod();
        String url = requestSpec.getEndpoint();

        // Track invocation
        String key = method + ":" + url;
        invocationCounts.put(key, invocationCounts.getOrDefault(key, 0) + 1);

        // Simulate latency if configured
        simulateLatency(url);

        // Find matching rule
        Optional<MockRule> matchingRule = findMatchingRule(method, url);

        if (matchingRule.isPresent()) {
            try {
                Object response = matchingRule.get().produceResponse(
                        url,
                        requestSpec.getBody()
                );

                // Convert the response to the expected type
                if (requestSpec.getResponseType().isInstance(response)) {
                    return (Res) response;
                } else if (response != null) {
                    // Try to convert using ObjectMapper if types don't match directly
                    return objectMapper.convertValue(response, requestSpec.getResponseType());
                }

                throw new ApiException("Mock response could not be converted to required type: "
                        + requestSpec.getResponseType().getName());
            } catch (Exception e) {
                if (e instanceof ApiException) {
                    throw (ApiException) e;
                }
                throw new ApiException("Failed to process mock response", e);
            }
        }

        // If we get here, no matching mock was found
        logger.warn("No mock response found for {} : {}", method, url);
        throw new ApiException("No mock response configured for " + method + ":" + url);
    }

    /**
     * Simulates network latency based on configuration
     */
    private void simulateLatency(String url) {
        long latency = latencies.entrySet().stream()
                .filter(entry -> Pattern.compile(entry.getKey()).matcher(url).matches())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0L);

        if (latency > 0) {
            try {
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void configureSsl(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public void setBaseUrl(String baseUrl) {

    }
} 