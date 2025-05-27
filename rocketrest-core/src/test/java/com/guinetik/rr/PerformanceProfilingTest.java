package com.guinetik.rr;

import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

/**
 * Performance profiling tests for RocketRest
 * Tests HTTP request latency using https://httpstat.us service.
 */
public class PerformanceProfilingTest {
    
    private RocketRest client;
    private static final String API_BASE_URL = "https://httpstat.us";
    private static final int DEFAULT_SAMPLE_SIZE = 10;
    private static final int WARM_UP_REQUESTS = 3;
    private static final int DIRECT_COMPARISON_SAMPLES = 50; // More samples for direct comparison
    
    @Before
    public void setUp() {
        // Create a vanilla client with default settings
        RocketRestConfig config = RocketRestConfig.builder(API_BASE_URL).build();
        client = new RocketRest(config);
    }
    
    @After
    public void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }
    
    /**
     * Tests the performance of synchronous GET requests.
     * This provides a baseline for comparison.
     */
    @Test
    public void testSyncClientPerformance() {
        System.out.println("=== Synchronous Client Performance Test ===");
        
        // Warm up the JVM and network
        warmUpRequests();
        
        // Measure GET /200 requests
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < DEFAULT_SAMPLE_SIZE; i++) {
            Instant start = Instant.now();
            try {
                String response = client.sync().get("/200", String.class);
                assertNotNull("Response should not be null", response);
                assertTrue("Response should contain 200", response.contains("200"));
            } catch (Exception e) {
                fail("Request failed: " + e.getMessage());
            }
            long elapsed = Duration.between(start, Instant.now()).toMillis();
            latencies.add(elapsed);
            
            // Small delay between requests to avoid rate limiting
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Report metrics
        reportLatencyMetrics("Sync Client GET /200", latencies);
    }
    
    /**
     * Tests the performance of asynchronous GET requests.
     */
    @Test
    public void testAsyncClientPerformance() {
        System.out.println("=== Asynchronous Client Performance Test ===");
        
        // Warm up
        warmUpRequests();
        
        // Measure GET /200 requests
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < DEFAULT_SAMPLE_SIZE; i++) {
            Instant start = Instant.now();
            try {
                CompletableFuture<String> future = client.async().get("/200", String.class);
                String response = future.get(5, TimeUnit.SECONDS);
                assertNotNull("Response should not be null", response);
                assertTrue("Response should contain 200", response.contains("200"));
            } catch (Exception e) {
                fail("Request failed: " + e.getMessage());
            }
            long elapsed = Duration.between(start, Instant.now()).toMillis();
            latencies.add(elapsed);
            
            // Small delay between requests
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Report metrics
        reportLatencyMetrics("Async Client GET /200", latencies);
    }
    
    /**
     * Tests the performance of the fluent API with Result pattern.
     */
    @Test
    public void testFluentClientPerformance() {
        System.out.println("=== Fluent Client Performance Test ===");
        
        // Warm up
        warmUpRequests();
        
        // Measure GET /200 requests
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < DEFAULT_SAMPLE_SIZE; i++) {
            Instant start = Instant.now();
            Result<String, ApiError> result = client.fluent().get("/200", String.class);
            long elapsed = Duration.between(start, Instant.now()).toMillis();
            
            assertTrue("Request should succeed", result.isSuccess());
            String response = result.getValue();
            assertNotNull("Response should not be null", response);
            assertTrue("Response should contain 200", response.contains("200"));
            
            latencies.add(elapsed);
            
            // Small delay between requests
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Report metrics
        reportLatencyMetrics("Fluent Client GET /200", latencies);
    }
    
    /**
     * Tests the performance impact of the circuit breaker.
     */
    @Test
    public void testCircuitBreakerClientPerformance() {
        System.out.println("=== Circuit Breaker Client Performance Test ===");
        
        // Create a new client with circuit breaker enabled
        RocketRestConfig cbConfig = RocketRestConfig.builder(API_BASE_URL)
                .defaultOptions(options -> {
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_ENABLED, true);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_THRESHOLD, 5);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_RESET_TIMEOUT_MS, 5000);
                })
                .build();
        
        RocketRest cbClient = new RocketRest(cbConfig);
        
        try {
            // Warm up
            for (int i = 0; i < WARM_UP_REQUESTS; i++) {
                cbClient.get("/200", String.class);
                Thread.sleep(100);
            }
            
            // Measure GET /200 requests
            List<Long> latencies = new ArrayList<>();
            
            for (int i = 0; i < DEFAULT_SAMPLE_SIZE; i++) {
                Instant start = Instant.now();
                String response = cbClient.get("/200", String.class);
                long elapsed = Duration.between(start, Instant.now()).toMillis();
                
                assertNotNull("Response should not be null", response);
                assertTrue("Response should contain 200", response.contains("200"));
                
                latencies.add(elapsed);
                
                // Small delay between requests
                Thread.sleep(100);
            }
            
            // Report metrics
            reportLatencyMetrics("Circuit Breaker Client GET /200", latencies);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        } finally {
            cbClient.shutdown();
        }
    }
    
    /**
     * Tests performance under load by making concurrent requests.
     */
    @Test
    public void testConcurrentRequestsPerformance() {
        System.out.println("=== Concurrent Requests Performance Test ===");
        
        // Warm up
        warmUpRequests();
        
        // Number of concurrent requests
        final int CONCURRENT_REQUESTS = 10;
        
        // Create futures for all requests
        List<CompletableFuture<String>> futures = new ArrayList<>();
        Instant start = Instant.now();
        
        // Launch concurrent requests
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            futures.add(client.async().get("/200", String.class));
        }
        
        // Wait for all to complete
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            allFutures.get(30, TimeUnit.SECONDS);
            
            // Check all responses
            for (CompletableFuture<String> future : futures) {
                String response = future.get();
                assertNotNull("Response should not be null", response);
                assertTrue("Response should contain 200", response.contains("200"));
            }
            
            long totalDuration = Duration.between(start, Instant.now()).toMillis();
            System.out.println("Concurrent performance (" + CONCURRENT_REQUESTS + " requests):");
            System.out.println("  Total time: " + totalDuration + "ms");
            System.out.println("  Avg time per request: " + (totalDuration / (double)CONCURRENT_REQUESTS) + "ms");
            System.out.println("  Throughput: " + (CONCURRENT_REQUESTS * 1000.0 / totalDuration) + " req/sec");
            
        } catch (Exception e) {
            fail("Concurrent requests failed: " + e.getMessage());
        }
    }
    
    /**
     * Tests performance with different payload sizes
     */
    @Test
    public void testPayloadSizeImpact() {
        System.out.println("=== Payload Size Impact Test ===");
        
        // Test different delay values (which affect response size)
        int[] delays = {0, 100, 500, 1000};
        
        for (int delay : delays) {
            List<Long> latencies = new ArrayList<>();
            
            for (int i = 0; i < DEFAULT_SAMPLE_SIZE; i++) {
                Instant start = Instant.now();
                try {
                    // The delay parameter affects how long the server waits before responding
                    // And slightly increases the response payload size
                    String response = client.sync().get("/200?sleep=" + delay, String.class);
                    assertNotNull("Response should not be null", response);
                } catch (Exception e) {
                    fail("Request failed: " + e.getMessage());
                }
                long elapsed = Duration.between(start, Instant.now()).toMillis();
                // Subtract the artificial delay to measure just processing time
                elapsed = elapsed - delay;
                latencies.add(elapsed);
                
                // Small delay between requests
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Report metrics
            reportLatencyMetrics("GET /200 with " + delay + "ms delay", latencies);
        }
    }
    
    /**
     * Warm up the JVM and network connections
     */
    private void warmUpRequests() {
        System.out.println("Warming up with " + WARM_UP_REQUESTS + " requests...");
        try {
            for (int i = 0; i < WARM_UP_REQUESTS; i++) {
                client.get("/200", String.class);
                Thread.sleep(100); // Small delay between requests
            }
        } catch (Exception e) {
            System.err.println("Warm-up failed: " + e.getMessage());
        }
    }
    
    /**
     * Reports various latency metrics for a test
     */
    private void reportLatencyMetrics(String testName, List<Long> latencies) {
        if (latencies.isEmpty()) {
            System.out.println(testName + ": No data collected");
            return;
        }
        
        LongSummaryStatistics stats = latencies.stream().collect(Collectors.summarizingLong(Long::longValue));
        
        // Calculate percentiles
        List<Long> sortedLatencies = latencies.stream().sorted().collect(Collectors.toList());
        long p50 = percentile(sortedLatencies, 50);
        long p90 = percentile(sortedLatencies, 90);
        long p95 = percentile(sortedLatencies, 95);
        
        System.out.println(testName + " Latency Metrics (ms):");
        System.out.println("  Samples: " + stats.getCount());
        System.out.println("  Min: " + stats.getMin());
        System.out.println("  Max: " + stats.getMax());
        System.out.println("  Avg: " + String.format("%.2f", stats.getAverage()));
        System.out.println("  P50: " + p50);
        System.out.println("  P90: " + p90);
        System.out.println("  P95: " + p95);
        System.out.println("  Raw data: " + latencies);
    }
    
    /**
     * Calculate percentile value from a sorted list
     */
    private long percentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(sortedValues.size() - 1, index));
        return sortedValues.get(index);
    }
    
    /**
     * This test specifically measures the overhead of the circuit breaker pattern
     * by directly comparing standard client vs circuit breaker client performance.
     * It uses a larger sample size for statistical significance.
     */
    @Test
    public void testCircuitBreakerOverhead() {
        System.out.println("=== Circuit Breaker Overhead Analysis ===");
        
        // Create two clients - one standard and one with circuit breaker
        RocketRestConfig standardConfig = RocketRestConfig.builder(API_BASE_URL).build();
        RocketRest standardClient = new RocketRest(standardConfig);
        
        RocketRestConfig cbConfig = RocketRestConfig.builder(API_BASE_URL)
                .defaultOptions(options -> {
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_ENABLED, true);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_FAILURE_THRESHOLD, 5);
                    options.set(HttpConstants.CircuitBreaker.CIRCUIT_BREAKER_RESET_TIMEOUT_MS, 5000);
                })
                .build();
        RocketRest cbClient = new RocketRest(cbConfig);
        
        try {
            // Warm up both clients
            System.out.println("Warming up clients...");
            for (int i = 0; i < WARM_UP_REQUESTS; i++) {
                standardClient.get("/200", String.class);
                cbClient.get("/200", String.class);
                Thread.sleep(50);
            }
            
            // Measure standard client performance
            List<Long> standardLatencies = new ArrayList<>();
            System.out.println("Measuring standard client performance with " + 
                    DIRECT_COMPARISON_SAMPLES + " samples...");
            
            for (int i = 0; i < DIRECT_COMPARISON_SAMPLES; i++) {
                Instant start = Instant.now();
                String response = standardClient.get("/200", String.class);
                long elapsed = Duration.between(start, Instant.now()).toMillis();
                standardLatencies.add(elapsed);
                
                // Small delay between requests
                Thread.sleep(50);
            }
            
            // Measure circuit breaker client performance
            List<Long> cbLatencies = new ArrayList<>();
            System.out.println("Measuring circuit breaker client performance with " + 
                    DIRECT_COMPARISON_SAMPLES + " samples...");
            
            for (int i = 0; i < DIRECT_COMPARISON_SAMPLES; i++) {
                Instant start = Instant.now();
                String response = cbClient.get("/200", String.class);
                long elapsed = Duration.between(start, Instant.now()).toMillis();
                cbLatencies.add(elapsed);
                
                // Small delay between requests
                Thread.sleep(50);
            }
            
            // Report metrics
            reportLatencyMetrics("Standard Client", standardLatencies);
            reportLatencyMetrics("Circuit Breaker Client", cbLatencies);
            
            // Calculate overhead
            double standardAvg = standardLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
            double cbAvg = cbLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
            double overheadMs = cbAvg - standardAvg;
            double overheadPercent = (overheadMs / standardAvg) * 100;
            
            System.out.println("\nCircuit Breaker Overhead Analysis:");
            System.out.println("  Standard Client Avg: " + String.format("%.2f", standardAvg) + "ms");
            System.out.println("  Circuit Breaker Avg: " + String.format("%.2f", cbAvg) + "ms");
            System.out.println("  Overhead: " + String.format("%.2f", overheadMs) + "ms (" + 
                    String.format("%.2f", overheadPercent) + "%)");
            
            // Run statistical significance test
            boolean isSignificant = runStatisticalSignificanceTest(standardLatencies, cbLatencies);
            System.out.println("  Statistically significant difference: " + isSignificant);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        } finally {
            standardClient.shutdown();
            cbClient.shutdown();
        }
    }
    
    /**
     * Performs a simple t-test to determine if the difference between
     * two sets of latency measurements is statistically significant.
     * This is a very basic implementation and not a rigorous statistical test.
     */
    private boolean runStatisticalSignificanceTest(List<Long> set1, List<Long> set2) {
        // Calculate means
        double mean1 = set1.stream().mapToLong(Long::longValue).average().orElse(0);
        double mean2 = set2.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // Calculate variances
        double variance1 = calculateVariance(set1, mean1);
        double variance2 = calculateVariance(set2, mean2);
        
        // Calculate t-statistic
        double n1 = set1.size();
        double n2 = set2.size();
        double t = Math.abs(mean1 - mean2) / 
                Math.sqrt((variance1 / n1) + (variance2 / n2));
        
        // Degrees of freedom (simplified)
        double df = n1 + n2 - 2;
        
        // Critical t-value for 95% confidence (simplified)
        double criticalT = 1.96; // Approximate critical t for large df
        
        System.out.println("  t-statistic: " + String.format("%.4f", t));
        System.out.println("  critical t-value (95% confidence): " + criticalT);
        
        return t > criticalT;
    }
    
    /**
     * Calculate variance of a set of values
     */
    private double calculateVariance(List<Long> values, double mean) {
        double sumSquaredDifferences = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();
        return sumSquaredDifferences / (values.size() - 1);
    }
}