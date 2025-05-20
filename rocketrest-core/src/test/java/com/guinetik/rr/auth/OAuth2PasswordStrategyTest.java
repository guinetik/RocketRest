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
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OAuth2PasswordStrategy}.
 */
public class OAuth2PasswordStrategyTest {

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpass";
    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String TOKEN_URL = "https://auth.example.com/token";
    private static final String REFRESH_TOKEN = "test-refresh-token";
    
    private OAuth2PasswordStrategy strategy;
    
    @Before
    public void setUp() {
        strategy = new OAuth2PasswordStrategy(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET, TOKEN_URL);
    }
    
    @Test
    public void testAuthTypeIsPassword() {
        assertEquals(AuthStrategy.AuthType.OAUTH_PASSWORD, strategy.getType());
    }
    
    @Test
    public void testConstructorWithAdditionalParams() {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("audience", "https://api.example.com");
        
        OAuth2PasswordStrategy strategyWithParams = 
            new OAuth2PasswordStrategy(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET, TOKEN_URL, additionalParams);
            
        assertNotNull(strategyWithParams);
        assertEquals(AuthStrategy.AuthType.OAUTH_PASSWORD, strategyWithParams.getType());
    }
    
    @Test(expected = TokenRefreshException.class)
    public void testValidateCredentialsFailsWithNullUsername() {
        OAuth2PasswordStrategy invalidStrategy = 
            new OAuth2PasswordStrategy(null, PASSWORD, CLIENT_ID, CLIENT_SECRET, TOKEN_URL);
        
        invalidStrategy.refreshCredentials();
    }
    
    @Test(expected = TokenRefreshException.class)
    public void testValidateCredentialsFailsWithNullPassword() {
        OAuth2PasswordStrategy invalidStrategy = 
            new OAuth2PasswordStrategy(USERNAME, null, CLIENT_ID, CLIENT_SECRET, TOKEN_URL);
        
        invalidStrategy.refreshCredentials();
    }
    
    @Test
    public void testValidateCredentialsSucceedsWithNullClientId() {
        // This should not throw an exception, as client ID is optional
        OAuth2PasswordStrategy validStrategy = 
            new OAuth2PasswordStrategy(USERNAME, PASSWORD, null, null, TOKEN_URL);
        
        // We just need to verify the validateCredentials method doesn't throw
        // We'll use reflection to call it directly
        try {
            Method validateMethod = AbstractOAuth2Strategy.class.getDeclaredMethod("validateCredentials");
            validateMethod.setAccessible(true);
            validateMethod.invoke(validStrategy);
        } catch (Exception e) {
            fail("validateCredentials should not throw with null clientId: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareTokenRequestParamsForPasswordGrant() throws Exception {
        Method prepareMethod = OAuth2PasswordStrategy.class.getDeclaredMethod("prepareTokenRequestParams");
        prepareMethod.setAccessible(true);
        
        Map<String, String> params = (Map<String, String>) prepareMethod.invoke(strategy);
        
        assertEquals("password", params.get("grant_type"));
        assertEquals(USERNAME, params.get("username"));
        assertEquals(PASSWORD, params.get("password"));
        assertEquals(CLIENT_ID, params.get("client_id"));
        assertEquals(CLIENT_SECRET, params.get("client_secret"));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareTokenRequestParamsWithoutClientCredentials() throws Exception {
        OAuth2PasswordStrategy strategyWithoutClient = 
            new OAuth2PasswordStrategy(USERNAME, PASSWORD, null, null, TOKEN_URL);
            
        Method prepareMethod = OAuth2PasswordStrategy.class.getDeclaredMethod("prepareTokenRequestParams");
        prepareMethod.setAccessible(true);
        
        Map<String, String> params = (Map<String, String>) prepareMethod.invoke(strategyWithoutClient);
        
        assertEquals("password", params.get("grant_type"));
        assertEquals(USERNAME, params.get("username"));
        assertEquals(PASSWORD, params.get("password"));
        assertFalse(params.containsKey("client_id"));
        assertFalse(params.containsKey("client_secret"));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareTokenRequestParamsWithRefreshToken() throws Exception {
        // Set a refresh token via reflection
        Field refreshTokenField = OAuth2PasswordStrategy.class.getDeclaredField("refreshToken");
        refreshTokenField.setAccessible(true);
        refreshTokenField.set(strategy, REFRESH_TOKEN);
        
        Method prepareMethod = OAuth2PasswordStrategy.class.getDeclaredMethod("prepareTokenRequestParams");
        prepareMethod.setAccessible(true);
        
        Map<String, String> params = (Map<String, String>) prepareMethod.invoke(strategy);
        
        assertEquals("refresh_token", params.get("grant_type"));
        assertEquals(REFRESH_TOKEN, params.get("refresh_token"));
        assertEquals(CLIENT_ID, params.get("client_id"));
        assertEquals(CLIENT_SECRET, params.get("client_secret"));
        // Should not include username/password when using refresh token
        assertFalse(params.containsKey("username"));
        assertFalse(params.containsKey("password"));
    }
    
    @Test
    public void testProcessTokenResponseWithRefreshToken() throws Exception {
        // Create a test token response with refresh token
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "new-access-token");
        tokenResponse.put("refresh_token", "new-refresh-token");
        tokenResponse.put("expires_in", "3600");
        
        // Call processTokenResponse via reflection
        Method processMethod = OAuth2PasswordStrategy.class.getDeclaredMethod("processTokenResponse", Map.class);
        processMethod.setAccessible(true);
        boolean result = (boolean) processMethod.invoke(strategy, tokenResponse);
        
        assertTrue(result);
        assertEquals("new-refresh-token", strategy.getRefreshToken());
    }
    
    @Test
    public void testApplyAuthHeadersWithToken() {
        // Setup: create a mock to test the inherited applyAuthHeaders method
        OAuth2PasswordStrategy spyStrategy = Mockito.spy(strategy);
        
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
    }
} 