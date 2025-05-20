package com.guinetik.rr.auth;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication strategy that implements OAuth 2.0 password flow.
 * This strategy gets and refreshes OAuth 2.0 access tokens using username and password.
 */
public class OAuth2PasswordStrategy extends AbstractOAuth2Strategy {

    private final String username;
    private final String password;
    private final String clientId;
    private final String clientSecret;
    private String refreshToken;

    /**
     * Creates a new OAuth 2.0 password strategy.
     *
     * @param username     the user's username
     * @param password     the user's password
     * @param clientId     the OAuth 2.0 client ID (optional, can be null)
     * @param clientSecret the OAuth 2.0 client secret (optional, can be null)
     * @param tokenUrl     the OAuth 2.0 token endpoint URL
     */
    public OAuth2PasswordStrategy(String username, String password, String clientId, String clientSecret, String tokenUrl) {
        this(username, password, clientId, clientSecret, tokenUrl, new HashMap<>());
    }

    /**
     * Creates a new OAuth 2.0 password strategy with additional parameters.
     *
     * @param username         the user's username
     * @param password         the user's password
     * @param clientId         the OAuth 2.0 client ID (optional, can be null)
     * @param clientSecret     the OAuth 2.0 client secret (optional, can be null)
     * @param tokenUrl         the OAuth 2.0 token endpoint URL
     * @param additionalParams additional parameters to include in the token request
     */
    public OAuth2PasswordStrategy(String username, String password, String clientId, String clientSecret,
                                  String tokenUrl,
                                  Map<String, String> additionalParams) {
        super(tokenUrl, additionalParams);
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public AuthType getType() {
        return AuthType.OAUTH_PASSWORD;
    }

    /**
     * {@inheritDoc}
     * @throws TokenRefreshException if the username or password is not provided.
     */
    @Override
    protected void validateCredentials() {
        if (username == null || password == null) {
            throw new TokenRefreshException("Username and Password are required for OAuth2 password flow");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Prepares parameters for the OAuth 2.0 password grant type or refresh token grant type.
     * If a refresh token is available, it will be used. Otherwise, username and password will be used.
     * Client ID and client secret are included if provided.
     */
    @Override
    protected Map<String, String> prepareTokenRequestParams() {
        Map<String, String> formParams = new HashMap<>();

        // If we have a refresh token, use that; otherwise, use password flow
        if (this.refreshToken != null && !this.refreshToken.isEmpty()) {
            formParams.put("grant_type", "refresh_token");
            formParams.put("refresh_token", this.refreshToken);
        } else {
            formParams.put("grant_type", "password");
            formParams.put("username", username);
            formParams.put("password", password);
        }

        // Add client credentials if provided
        if (clientId != null && !clientId.isEmpty()) {
            formParams.put("client_id", clientId);

            if (clientSecret != null && !clientSecret.isEmpty()) {
                formParams.put("client_secret", clientSecret);
            }
        }

        return formParams;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Processes the token response, extracting and storing the refresh_token if present,
     * in addition to the access_token and expiry time handled by the superclass.
     */
    @Override
    protected boolean processTokenResponse(Map<String, Object> tokenResponse) {
        Object refreshTokenObj = tokenResponse.get("refresh_token");

        // Update refresh token if provided
        if (refreshTokenObj != null) {
            this.refreshToken = refreshTokenObj.toString();
        }

        // Let the parent class handle the rest
        return super.processTokenResponse(tokenResponse);
    }

    /**
     * Gets the current refresh token.
     *
     * @return the current refresh token, or null if not yet obtained
     */
    public String getRefreshToken() {
        return refreshToken;
    }
}