package com.guinetik.rr.auth;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Generic tests for the {@link AuthStrategy} contract that apply to all simple strategies.
 */
public class AuthStrategyTest {

    @Test
    public void testAuthTypeEnumValues() {
        // Ensure enum contains expected values
        assertNotNull(AuthStrategy.AuthType.valueOf("NONE"));
        assertNotNull(AuthStrategy.AuthType.valueOf("BEARER_TOKEN"));
        assertNotNull(AuthStrategy.AuthType.valueOf("BASIC"));
    }

    @Test
    public void testNeedsTokenRefreshDefaultBehaviour() {
        AuthStrategy noAuth = new NoAuthStrategy();
        assertFalse(noAuth.needsTokenRefresh());
    }
} 