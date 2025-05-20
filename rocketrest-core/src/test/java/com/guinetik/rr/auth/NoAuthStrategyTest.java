package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NoAuthStrategy}.
 */
public class NoAuthStrategyTest {

    private final NoAuthStrategy strategy = new NoAuthStrategy();

    @Test
    public void testApplyAuthHeadersDoesNotModifyHeaders() {
        RocketHeaders headers = RocketHeaders.defaultJson();
        RocketHeaders result = strategy.applyAuthHeaders(headers);
        assertSame("Should return same headers instance", headers, result);
        assertFalse("Authorization header should not be present", result.contains(RocketHeaders.Names.AUTHORIZATION));
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