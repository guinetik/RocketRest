package com.guinetik.rr.auth;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AuthStrategyFactory} helper methods (non-OAuth variants).
 */
public class AuthStrategyFactoryTest {

    @Test
    public void testCreateNoAuth() {
        AuthStrategy strategy = AuthStrategyFactory.createNoAuth();
        assertTrue(strategy instanceof NoAuthStrategy);
    }

    @Test
    public void testCreateBasicAuth() {
        AuthStrategy strategy = AuthStrategyFactory.createBasicAuth("user", "pass");
        assertTrue(strategy instanceof BasicAuthStrategy);
    }

    @Test
    public void testCreateBearerToken() {
        AuthStrategy strategy = AuthStrategyFactory.createBearerToken("token");
        assertTrue(strategy instanceof BearerTokenStrategy);
    }

    @Test
    public void testCreateBearerTokenWithCustomLogic() {
        AuthStrategy strategy = AuthStrategyFactory.createBearerToken("token", () -> true);
        assertTrue(strategy instanceof BearerTokenStrategy);
        assertTrue(strategy.refreshCredentials());
    }

    @Test
    public void testCreateOAuth2ClientCredentialsRequiresTokenUrl() {
        String tokenUrl = "https://auth.example.com/token";
        AuthStrategy strategy = AuthStrategyFactory.createOAuth2ClientCredentials("id", "secret", tokenUrl);
        assertEquals(AuthStrategy.AuthType.OAUTH_CLIENT_CREDENTIALS, strategy.getType());
    }
} 