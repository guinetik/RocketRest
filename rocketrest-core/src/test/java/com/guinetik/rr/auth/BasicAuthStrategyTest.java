package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BasicAuthStrategy}.
 */
public class BasicAuthStrategyTest {

    private static final String USERNAME = "alice";
    private static final String PASSWORD = "secret";

    private final BasicAuthStrategy strategy = new BasicAuthStrategy(USERNAME, PASSWORD);

    @Test
    public void testApplyAuthHeadersSetsBasicAuthorization() {
        RocketHeaders headers = new RocketHeaders();
        strategy.applyAuthHeaders(headers);
        assertTrue(headers.contains(RocketHeaders.Names.AUTHORIZATION));
        String expectedPrefix = "Basic ";
        assertTrue(headers.get(RocketHeaders.Names.AUTHORIZATION).startsWith(expectedPrefix));
    }

    @Test
    public void testNeedsRefresh() {
        assertFalse(strategy.needsTokenRefresh());
    }

    @Test
    public void testRefreshCredentialsAlwaysTrue() {
        assertTrue(strategy.refreshCredentials());
    }
} 