package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link OAuth2ClientCredentialsStrategy}.
 */
public class OAuth2ClientCredentialsStrategyTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String TOKEN_URL = "https://auth.example.com/token";
    
    private OAuth2ClientCredentialsStrategy strategy;
    
    @Before
    public void setUp() {
        strategy = new OAuth2ClientCredentialsStrategy(CLIENT_ID, CLIENT_SECRET, TOKEN_URL);
    }
    
    @Test
    public void testAuthTypeIsClientCredentials() {
        assertEquals(AuthStrategy.AuthType.OAUTH_CLIENT_CREDENTIALS, strategy.getType());
    }
    
    @Test
    public void testConstructorWithAdditionalParams() {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("audience", "https://api.example.com");
        
        OAuth2ClientCredentialsStrategy strategyWithParams = 
            new OAuth2ClientCredentialsStrategy(CLIENT_ID, CLIENT_SECRET, TOKEN_URL, additionalParams);
            
        // Verify the constructor creates a valid object
        assertNotNull(strategyWithParams);
        assertEquals(AuthStrategy.AuthType.OAUTH_CLIENT_CREDENTIALS, strategyWithParams.getType());
    }
    
    @Test(expected = TokenRefreshException.class)
    public void testValidateCredentialsFailsWithNullClientId() {
        // Create strategy with null client ID
        OAuth2ClientCredentialsStrategy invalidStrategy = 
            new OAuth2ClientCredentialsStrategy(null, CLIENT_SECRET, TOKEN_URL);
        
        // This should throw TokenRefreshException
        invalidStrategy.refreshCredentials();
    }
    
    @Test(expected = TokenRefreshException.class)
    public void testValidateCredentialsFailsWithNullClientSecret() {
        // Create strategy with null client secret
        OAuth2ClientCredentialsStrategy invalidStrategy = 
            new OAuth2ClientCredentialsStrategy(CLIENT_ID, null, TOKEN_URL);
        
        // This should throw TokenRefreshException
        invalidStrategy.refreshCredentials();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareTokenRequestParams() throws Exception {
        // Use reflection to access the protected method
        Method prepareMethod = OAuth2ClientCredentialsStrategy.class.getDeclaredMethod("prepareTokenRequestParams");
        prepareMethod.setAccessible(true);
        
        Map<String, String> params = (Map<String, String>) prepareMethod.invoke(strategy);
        
        assertEquals("client_credentials", params.get("grant_type"));
        assertEquals(CLIENT_ID, params.get("client_id"));
        assertEquals(CLIENT_SECRET, params.get("client_secret"));
    }
    
    @Test
    public void testApplyAuthHeadersWithToken() {
        // Setup: create a mock to test the inherited applyAuthHeaders method
        OAuth2ClientCredentialsStrategy spyStrategy = Mockito.spy(strategy);
        
        // Set an access token via reflection
        try {
            Field accessTokenField = AbstractOAuth2Strategy.class.getDeclaredField("accessToken");
            accessTokenField.setAccessible(true);
            accessTokenField.set(spyStrategy, "test-token");
        } catch (Exception e) {
            fail("Could not set access token field: " + e.getMessage());
        }
        
        RocketHeaders headers = new RocketHeaders();
        spyStrategy.applyAuthHeaders(headers);
        
        assertTrue(headers.contains(RocketHeaders.Names.AUTHORIZATION));
        assertEquals("Bearer test-token", headers.get(RocketHeaders.Names.AUTHORIZATION));
    }
    
    @Test
    public void testNeedsTokenRefresh() throws Exception {
        // Initially should need refresh (no token)
        assertTrue(strategy.needsTokenRefresh());
        
        // Set a token but no expiry - should still need refresh
        Field accessTokenField = AbstractOAuth2Strategy.class.getDeclaredField("accessToken");
        accessTokenField.setAccessible(true);
        accessTokenField.set(strategy, "test-token");
        assertTrue(strategy.needsTokenRefresh());
        
        // Set expiry in the future - should not need refresh
        Field expiryField = AbstractOAuth2Strategy.class.getDeclaredField("tokenExpiryTime");
        expiryField.setAccessible(true);
        // Set expiry to 1 hour from now
        expiryField.set(strategy, Date.from(Instant.now().plusSeconds(3600)));
        assertFalse(strategy.needsTokenRefresh());
        
        // Set expiry to near future (< 5 min) - should need refresh
        expiryField.set(strategy, Date.from(Instant.now().plusSeconds(200)));
        assertTrue(strategy.needsTokenRefresh());
        
        // Set expiry to past - should need refresh
        expiryField.set(strategy, Date.from(Instant.now().minusSeconds(100)));
        assertTrue(strategy.needsTokenRefresh());
    }
} 