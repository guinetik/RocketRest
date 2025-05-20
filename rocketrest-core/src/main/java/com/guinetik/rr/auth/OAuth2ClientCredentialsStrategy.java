package com.guinetik.rr.auth;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication strategy that implements OAuth 2.0 client credentials flow.
 * This strategy gets and refreshes OAuth 2.0 access tokens using client credentials.
 */
public class OAuth2ClientCredentialsStrategy extends AbstractOAuth2Strategy {

    private final String clientId;
    private final String clientSecret;

    /**
     * Creates a new OAuth 2.0 client credentials strategy.
     *
     * @param clientId     the OAuth 2.0 client ID
     * @param clientSecret the OAuth 2.0 client secret
     * @param tokenUrl     the OAuth 2.0 token endpoint URL
     */
    public OAuth2ClientCredentialsStrategy(String clientId, String clientSecret, String tokenUrl) {
        this(clientId, clientSecret, tokenUrl, new HashMap<>());
    }

    /**
     * Creates a new OAuth 2.0 client credentials strategy with additional parameters.
     *
     * @param clientId         the OAuth 2.0 client ID
     * @param clientSecret     the OAuth 2.0 client secret
     * @param tokenUrl         the OAuth 2.0 token endpoint URL
     * @param additionalParams additional parameters to include in the token request
     */
    public OAuth2ClientCredentialsStrategy(String clientId, String clientSecret, String tokenUrl,
                                           Map<String, String> additionalParams) {
        super(tokenUrl, additionalParams);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public AuthType getType() {
        return AuthType.OAUTH_CLIENT_CREDENTIALS;
    }

    /**
     * {@inheritDoc}
     * @throws TokenRefreshException if the client ID or client secret is not provided.
     */
    @Override
    protected void validateCredentials() {
        if (clientId == null || clientSecret == null) {
            throw new TokenRefreshException("Client ID and Client Secret are required for OAuth2 client credentials flow");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Prepares parameters for the client credentials grant type, including
     * grant_type, client_id, and client_secret.
     */
    @Override
    protected Map<String, String> prepareTokenRequestParams() {
        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", "client_credentials");
        formParams.put("client_id", clientId);
        formParams.put("client_secret", clientSecret);
        return formParams;
    }
} 