package com.guinetik.rr.auth;

import com.guinetik.rr.http.RocketHeaders;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link OAuth2AssertionStrategy}.
 */
public class OAuth2AssertionStrategyTest {

    private static final String CLIENT_ID = "test-client";
    private static final String USER_ID = "test-user";
    private static final String PRIVATE_KEY = "test-private-key";
    private static final String COMPANY_ID = "test-company";
    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:saml2-bearer";
    private static final String ASSERTION_URL = "https://assertion.example.com/token";
    private static final String TOKEN_URL = "https://auth.example.com/token";
    private static final String TEST_ASSERTION = "test-assertion-value";

    private OAuth2AssertionStrategy strategy;

    /**
     * Testable subclass that allows overriding the post method for testing.
     */
    private static class TestableOAuth2AssertionStrategy extends OAuth2AssertionStrategy {
        private String postResponse = "";
        private String lastPostUrl;
        private Map<String, String> lastPostParams;

        public TestableOAuth2AssertionStrategy(String clientId, String userId, String privateKey,
                                                String companyId, String grantType, String assertionUrl, String tokenUrl) {
            super(clientId, userId, privateKey, companyId, grantType, assertionUrl, tokenUrl);
        }

        @Override
        protected String post(String url, Map<String, String> formParams) throws IOException {
            this.lastPostUrl = url;
            this.lastPostParams = new HashMap<>(formParams);
            return postResponse;
        }

        public void setPostResponse(String response) {
            this.postResponse = response;
        }

        public String getLastPostUrl() {
            return lastPostUrl;
        }

        public Map<String, String> getLastPostParams() {
            return lastPostParams;
        }
    }

    @Before
    public void setUp() {
        strategy = new OAuth2AssertionStrategy(
            CLIENT_ID, USER_ID, PRIVATE_KEY, COMPANY_ID, GRANT_TYPE, ASSERTION_URL, TOKEN_URL);
    }
    
    @Test
    public void testAuthTypeIsAssertion() {
        assertEquals(AuthStrategy.AuthType.OAUTH_ASSERTION, strategy.getType());
    }
    
    @Test
    public void testConstructorWithAdditionalParams() {
        Map<String, String> assertionParams = new HashMap<>();
        assertionParams.put("scope", "api");
        
        Map<String, String> tokenParams = new HashMap<>();
        tokenParams.put("audience", "https://api.example.com");
        
        OAuth2AssertionStrategy strategyWithParams = new OAuth2AssertionStrategy(
            CLIENT_ID, USER_ID, PRIVATE_KEY, COMPANY_ID, GRANT_TYPE, ASSERTION_URL, TOKEN_URL,
            assertionParams, tokenParams);
            
        assertNotNull(strategyWithParams);
        assertEquals(AuthStrategy.AuthType.OAUTH_ASSERTION, strategyWithParams.getType());
    }
    
    @Test(expected = TokenRefreshException.class)
    public void testValidateCredentialsFailsWithNullClientId() {
        OAuth2AssertionStrategy invalidStrategy = new OAuth2AssertionStrategy(
            null, USER_ID, PRIVATE_KEY, COMPANY_ID, GRANT_TYPE, ASSERTION_URL, TOKEN_URL);
        
        invalidStrategy.refreshCredentials();
    }
    
    @Test(expected = TokenRefreshException.class)
    public void testValidateCredentialsFailsWithNullUserId() {
        OAuth2AssertionStrategy invalidStrategy = new OAuth2AssertionStrategy(
            CLIENT_ID, null, PRIVATE_KEY, COMPANY_ID, GRANT_TYPE, ASSERTION_URL, TOKEN_URL);
        
        invalidStrategy.refreshCredentials();
    }
    
    @Test(expected = TokenRefreshException.class)
    public void testValidateCredentialsFailsWithNullPrivateKey() {
        OAuth2AssertionStrategy invalidStrategy = new OAuth2AssertionStrategy(
            CLIENT_ID, USER_ID, null, COMPANY_ID, GRANT_TYPE, ASSERTION_URL, TOKEN_URL);
        
        invalidStrategy.refreshCredentials();
    }
    
    @Test(expected = TokenRefreshException.class)
    public void testValidateCredentialsFailsWithNullGrantType() {
        OAuth2AssertionStrategy invalidStrategy = new OAuth2AssertionStrategy(
            CLIENT_ID, USER_ID, PRIVATE_KEY, COMPANY_ID, null, ASSERTION_URL, TOKEN_URL);
        
        invalidStrategy.refreshCredentials();
    }
    
    @Test
    public void testValidateCredentialsSucceedsWithNullCompanyId() {
        // Company ID is optional according to the docs
        OAuth2AssertionStrategy validStrategy = new OAuth2AssertionStrategy(
            CLIENT_ID, USER_ID, PRIVATE_KEY, null, GRANT_TYPE, ASSERTION_URL, TOKEN_URL);
        
        // We just need to verify the validateCredentials method doesn't throw
        try {
            Method validateMethod = OAuth2AssertionStrategy.class.getDeclaredMethod("validateCredentials");
            validateMethod.setAccessible(true);
            validateMethod.invoke(validStrategy);
        } catch (Exception e) {
            fail("validateCredentials should not throw with null companyId: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareTokenRequestParams() throws Exception {
        Method prepareMethod = OAuth2AssertionStrategy.class.getDeclaredMethod("prepareTokenRequestParams");
        prepareMethod.setAccessible(true);
        
        Map<String, String> params = (Map<String, String>) prepareMethod.invoke(strategy);
        
        assertEquals(CLIENT_ID, params.get("client_id"));
        assertEquals(USER_ID, params.get("user_id"));
        assertEquals(GRANT_TYPE, params.get("grant_type"));
        assertEquals(COMPANY_ID, params.get("company_id"));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareTokenRequestParamsWithoutCompanyId() throws Exception {
        OAuth2AssertionStrategy strategyWithoutCompany = new OAuth2AssertionStrategy(
            CLIENT_ID, USER_ID, PRIVATE_KEY, null, GRANT_TYPE, ASSERTION_URL, TOKEN_URL);
            
        Method prepareMethod = OAuth2AssertionStrategy.class.getDeclaredMethod("prepareTokenRequestParams");
        prepareMethod.setAccessible(true);
        
        Map<String, String> params = (Map<String, String>) prepareMethod.invoke(strategyWithoutCompany);
        
        assertEquals(CLIENT_ID, params.get("client_id"));
        assertEquals(USER_ID, params.get("user_id"));
        assertEquals(GRANT_TYPE, params.get("grant_type"));
        assertFalse(params.containsKey("company_id"));
    }
    
    @Test
    public void testRefreshCredentialsWithEmptyAssertion() {
        // Create testable strategy with empty response
        TestableOAuth2AssertionStrategy testStrategy = new TestableOAuth2AssertionStrategy(
            CLIENT_ID, USER_ID, PRIVATE_KEY, COMPANY_ID, GRANT_TYPE, ASSERTION_URL, TOKEN_URL);
        testStrategy.setPostResponse("");

        // Call refreshCredentials
        boolean result = testStrategy.refreshCredentials();

        // Verify the expected behavior
        assertFalse(result);

        // Verify that post was called with assertion URL
        assertEquals(ASSERTION_URL, testStrategy.getLastPostUrl());
    }

    @Test
    public void testGetAssertion() throws Exception {
        // Create testable strategy
        TestableOAuth2AssertionStrategy testStrategy = new TestableOAuth2AssertionStrategy(
            CLIENT_ID, USER_ID, PRIVATE_KEY, COMPANY_ID, GRANT_TYPE, ASSERTION_URL, TOKEN_URL);
        testStrategy.setPostResponse(TEST_ASSERTION);

        // Call getAssertion via reflection
        Method getAssertionMethod = OAuth2AssertionStrategy.class.getDeclaredMethod("getAssertion");
        getAssertionMethod.setAccessible(true);
        String assertion = (String) getAssertionMethod.invoke(testStrategy);

        // Verify the expected behavior
        assertEquals(TEST_ASSERTION, assertion);

        // Verify post was called with the correct parameters
        Map<String, String> params = testStrategy.getLastPostParams();
        assertEquals(CLIENT_ID, params.get("client_id"));
        assertEquals(USER_ID, params.get("user_id"));
        assertEquals(TOKEN_URL, params.get("token_url"));
        assertEquals(PRIVATE_KEY, params.get("private_key"));
    }
    
    @Test
    public void testApplyAuthHeadersWithToken() {
        // Set an access token via reflection on the actual strategy (no spy needed)
        try {
            Field accessTokenField = AbstractOAuth2Strategy.class.getDeclaredField("accessToken");
            accessTokenField.setAccessible(true);
            accessTokenField.set(strategy, "test-token");
        } catch (Exception e) {
            fail("Could not set access token field: " + e.getMessage());
        }

        RocketHeaders headers = new RocketHeaders();
        strategy.applyAuthHeaders(headers);

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