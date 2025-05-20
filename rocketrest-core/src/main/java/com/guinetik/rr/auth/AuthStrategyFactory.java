package com.guinetik.rr.auth;

import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Factory for creating authentication strategies.
 */
public class AuthStrategyFactory {
    
    /**
     * Creates a strategy that does not perform authentication.
     * 
     * @return a new no-auth strategy
     */
    public static AuthStrategy createNoAuth() {
        return new NoAuthStrategy();
    }
    
    /**
     * Creates a strategy that uses HTTP Basic authentication.
     * 
     * @param username the username
     * @param password the password
     * @return a new basic auth strategy
     */
    public static AuthStrategy createBasicAuth(String username, String password) {
        return new BasicAuthStrategy(username, password);
    }
    
    /**
     * Creates a strategy that uses Bearer token authentication.
     * 
     * @param token the bearer token
     * @return a new bearer token strategy
     */
    public static AuthStrategy createBearerToken(String token) {
        return new BearerTokenStrategy(token);
    }
    
    /**
     * Creates a strategy that uses Bearer token authentication with custom refresh logic.
     * 
     * @param token             the bearer token
     * @param refreshTokenLogic a {@link BooleanSupplier} that dictates the behavior of token refresh.
     *                          It should return {@code true} if the token was successfully refreshed, {@code false} otherwise.
     * @return a new bearer token strategy with custom refresh logic
     */
    public static AuthStrategy createBearerToken(String token, BooleanSupplier refreshTokenLogic) {
        return new BearerTokenStrategy(token, refreshTokenLogic);
    }
    
    /**
     * Creates a strategy that uses OAuth 2.0 client credentials flow.
     * 
     * @param clientId the OAuth 2.0 client ID
     * @param clientSecret the OAuth 2.0 client secret
     * @return a new OAuth 2.0 client credentials strategy
     */
    public static AuthStrategy createOAuth2ClientCredentials(String clientId, String clientSecret, String tokenUrl) {
        return new OAuth2ClientCredentialsStrategy(clientId, clientSecret, tokenUrl);
    }
    
    /**
     * Creates a strategy that uses OAuth 2.0 client credentials flow with additional parameters.
     * 
     * @param clientId the OAuth 2.0 client ID
     * @param clientSecret the OAuth 2.0 client secret
     * @param tokenUrl the token endpoint URL
     * @param additionalParams additional parameters to include in the token request
     * @return a new OAuth 2.0 client credentials strategy
     */
    public static AuthStrategy createOAuth2ClientCredentials(String clientId, String clientSecret, String tokenUrl,
                                                           Map<String, String> additionalParams) {
        return new OAuth2ClientCredentialsStrategy(clientId, clientSecret, tokenUrl, additionalParams);
    }
    
    /**
     * Creates a strategy that uses OAuth 2.0 password flow.
     * 
     * @param username the user's username
     * @param password the user's password
     * @return a new OAuth 2.0 password strategy
     */
    public static AuthStrategy createOAuth2Password(String username, String password, String tokenUrl) {
        return new OAuth2PasswordStrategy(username, password, null, null, tokenUrl);
    }
    
    /**
     * Creates a strategy that uses OAuth 2.0 password flow with client credentials.
     * 
     * @param username the user's username
     * @param password the user's password
     * @param clientId the OAuth 2.0 client ID
     * @param clientSecret the OAuth 2.0 client secret
     * @return a new OAuth 2.0 password strategy
     */
    public static AuthStrategy createOAuth2Password(String username, String password, 
                                                  String clientId, String clientSecret, String tokenUrl) {
        return new OAuth2PasswordStrategy(username, password, clientId, clientSecret, tokenUrl);
    }
    
    /**
     * Creates a strategy that uses OAuth 2.0 password flow with client credentials and additional parameters.
     * 
     * @param username the user's username
     * @param password the user's password
     * @param clientId the OAuth 2.0 client ID
     * @param clientSecret the OAuth 2.0 client secret
     * @param tokenUrl the token endpoint URL
     * @param additionalParams additional parameters to include in the token request
     * @return a new OAuth 2.0 password strategy
     */
    public static AuthStrategy createOAuth2Password(String username, String password, 
                                                  String clientId, String clientSecret,
                                                  String tokenUrl,
                                                  Map<String, String> additionalParams) {
        return new OAuth2PasswordStrategy(username, password, clientId, clientSecret, tokenUrl, additionalParams);
    }

    /**
     * Creates a strategy that uses OAuth 2.0 assertion flow.
     * This can be used with various identity providers like SAP, Azure AD, Okta, etc.
     * 
     * @param clientId       the OAuth client ID
     * @param userId         the user ID
     * @param privateKey     the private key for assertion
     * @param companyId      the company ID (optional, can be null)
     * @param grantType      the OAuth grant type
     * @param assertionUrl   the assertion endpoint URL
     * @param tokenUrl       the token endpoint URL
     * @return a new OAuth 2.0 assertion strategy
     */
    public static AuthStrategy createOAuth2Assertion(String clientId, String userId, String privateKey,
                                           String companyId, String grantType, String assertionUrl, String tokenUrl) {
        return new OAuth2AssertionStrategy(clientId, userId, privateKey, companyId, grantType, assertionUrl, tokenUrl);
    }

    /**
     * Creates a strategy that uses OAuth 2.0 assertion flow with additional parameters.
     * This can be used with various identity providers like SAP, Azure AD, Okta, etc.
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
     * @return a new OAuth 2.0 assertion strategy
     */
    public static AuthStrategy createOAuth2Assertion(String clientId, String userId, String privateKey,
                                           String companyId, String grantType, String assertionUrl, String tokenUrl,
                                           Map<String, String> additionalAssertionParams,
                                           Map<String, String> additionalTokenParams) {
        return new OAuth2AssertionStrategy(clientId, userId, privateKey, companyId, grantType, assertionUrl, tokenUrl,
                                 additionalAssertionParams, additionalTokenParams);
    }
} 