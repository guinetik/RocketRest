package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BearerTokenStrategy}.
 */
public class BearerTokenStrategyTest {

    private static final String TOKEN = "abc123";

    @Test
    public void testApplyAuthHeadersSetsBearerAuthorization() {
        BearerTokenStrategy strategy = new BearerTokenStrategy(TOKEN);
        RocketHeaders headers = new RocketHeaders();
        strategy.applyAuthHeaders(headers);
        assertEquals("Bearer " + TOKEN, headers.get(RocketHeaders.Names.AUTHORIZATION));
    }

    @Test
    public void testRefreshCredentialsDefaultReturnsFalse() {
        BearerTokenStrategy strategy = new BearerTokenStrategy(TOKEN);
        assertFalse(strategy.refreshCredentials());
    }

    @Test
    public void testRefreshCredentialsCustomLogic() {
        AtomicBoolean called = new AtomicBoolean(false);
        BearerTokenStrategy strategy = new BearerTokenStrategy(TOKEN, () -> {
            called.set(true);
            return true;
        });
        assertTrue(strategy.refreshCredentials());
        assertTrue("Custom supplier should have been called", called.get());
    }
} 