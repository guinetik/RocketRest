package com.guinetik.rr.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication strategy that implements OAuth 2.0 assertion flow.
 * This strategy implements a two-step OAuth flow:
 * 1. Get an assertion from the Identity Provider endpoint by providing a private key;
 * 2. Use the assertion to get the actual OAuth token from the token endpoint.
 * This can be used with various identity providers like SAP, Azure AD, Okta, etc.
 */
public class OAuth2AssertionStrategy extends AbstractOAuth2Strategy {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AssertionStrategy.class);

    private final String clientId;
    private final String userId;
    private final String privateKey;
    private final String companyId;
    private final String grantType;
    private final String assertionUrl;
    private final String tokenUrl;
    private final Map<String, String> additionalAssertionParams;

    /**
     * Creates a new OAuth 2.0 assertion strategy.
     *
     * @param clientId       the OAuth client ID
     * @param userId         the user ID
     * @param privateKey     the private key for assertion
     * @param companyId      the company ID (optional, can be null)
     * @param grantType      the OAuth grant type
     * @param assertionUrl   the assertion endpoint URL
     * @param tokenUrl       the token endpoint URL
     */
    public OAuth2AssertionStrategy(String clientId, String userId, String privateKey, 
                          String companyId, String grantType, String assertionUrl, String tokenUrl) {
        this(clientId, userId, privateKey, companyId, grantType, assertionUrl, tokenUrl, 
             new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a new OAuth 2.0 assertion strategy with additional parameters.
     *
     * @param clientId                 the OAuth client ID
     * @param userId                   the user ID
     * @param privateKey               the private key for assertion
     * @param companyId                the company ID (optional, can be null)
     * @param grantType                the OAuth grant type
     * @param assertionUrl             the assertion endpoint URL
     * @param tokenUrl                 the token endpoint URL
     * @param additionalAssertionParams additional parameters for assertion request
     * @param additionalTokenParams    additional parameters for token request
     */
    public OAuth2AssertionStrategy(String clientId, String userId, String privateKey, 
                          String companyId, String grantType, String assertionUrl, String tokenUrl,
                          Map<String, String> additionalAssertionParams,
                          Map<String, String> additionalTokenParams) {
        super(tokenUrl, additionalTokenParams);
        this.clientId = clientId;
        this.userId = userId;
        this.privateKey = privateKey;
        this.companyId = companyId;
        this.grantType = grantType;
        this.assertionUrl = assertionUrl;
        this.tokenUrl = tokenUrl;
        this.additionalAssertionParams = additionalAssertionParams;
    }

    @Override
    public AuthType getType() {
        return AuthType.OAUTH_ASSERTION;
    }

    /**
     * {@inheritDoc}
     * @throws TokenRefreshException if any of the required parameters (clientId, userId, privateKey,
     * grantType, assertionUrl, or tokenUrl) are missing.
     */
    @Override
    protected void validateCredentials() {
        if (clientId == null || userId == null || privateKey == null || 
            grantType == null || assertionUrl == null || tokenUrl == null) {
            throw new TokenRefreshException("Required credentials are missing for OAuth 2.0 assertion flow");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation handles the two-step OAuth 2.0 assertion flow:
     * <ol>
     *   <li>It first calls {@link #getAssertion()} to obtain an assertion from the configured assertion URL.</li>
     *   <li>Then, it uses this assertion along with other parameters (clientId, userId, grantType, companyId if present)
     *       to call the {@code super.refreshCredentials()} method, which performs the actual token request
     *       to the configured token URL.</li>
     * </ol>
     *
     * @return {@code true} if the token was successfully refreshed, {@code false} otherwise.
     * @throws TokenRefreshException if token refresh fails at any step.
     */
    @Override
    public boolean refreshCredentials() {
        try {
            // Step 1: Get assertion from the assertion endpoint
            String assertion = getAssertion();
            if (assertion.isEmpty()) {
                logger.error("Failed to get assertion from assertion endpoint");
                return false;
            }
            // Step 2: Get token using assertion and the parent class functionality
            // Prepare token parameters including the assertion we just got
            additionalParams.put("client_id", clientId);
            additionalParams.put("user_id", userId);
            additionalParams.put("grant_type", grantType);
            if (companyId != null) {
                additionalParams.put("company_id", companyId);
            }
            additionalParams.put("assertion", assertion);
            // Call the parent implementation to get the token
            return super.refreshCredentials();
        } catch (Exception e) {
            logger.error("Failed to refresh token", e);
            throw new TokenRefreshException("Failed to refresh token: " + e.getMessage());
        }
    }

    /**
     * Retrieves an assertion token from the configured assertion URL.
     * This method makes a POST request to the {@code assertionUrl} using parameters such as
     * clientId, userId, the privateKey, and the target tokenUrl.
     *
     * @return The assertion string obtained from the endpoint.
     * @throws IOException if an I/O error occurs during the request to the assertion endpoint.
     * @throws TokenRefreshException if the assertion endpoint returns an error or an empty assertion.
     */
    private String getAssertion() throws IOException {
        Map<String, String> assertionParams = new HashMap<>(additionalAssertionParams);
        assertionParams.put("client_id", clientId);
        assertionParams.put("user_id", userId);
        assertionParams.put("token_url", tokenUrl);
        assertionParams.put("private_key", privateKey);

        // Make the request to the assertion endpoint using the parent class's POST method
        try {
            return post(assertionUrl, assertionParams).trim();
        } catch (Exception e) {
            logger.error("Error getting assertion", e);
            throw new IOException("Error getting assertion: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Prepares parameters for the token request part of the assertion flow.
     * This method is typically called by the parent class's {@code refreshToken} method.
     * It includes clientId, userId, grantType, and companyId (if available).
     * The assertion itself is expected to have been added to {@code additionalParams} by the
     * overridden {@link #refreshCredentials()} method before this is called.
     */
    @Override
    protected Map<String, String> prepareTokenRequestParams() {
        // This method is called by the parent class's refreshToken method
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("user_id", userId);
        params.put("grant_type", grantType);
        if (companyId != null) {
            params.put("company_id", companyId);
        }
        // The assertion should be in additionalParams at this point        
        return params;
    }
} 