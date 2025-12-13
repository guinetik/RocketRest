package com.guinetik.rr.auth;

import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Factory for creating {@link AuthStrategy} instances.
 *
 * <p>This factory provides static methods to create various authentication strategies
 * without directly instantiating the strategy classes. It's the recommended way to
 * create authentication strategies for use with {@link com.guinetik.rr.RocketRestConfig}.
 *
 * <h2>No Authentication</h2>
 * <pre class="language-java"><code>
 * AuthStrategy noAuth = AuthStrategyFactory.createNoAuth();
 * </code></pre>
 *
 * <h2>Basic Authentication</h2>
 * <pre class="language-java"><code>
 * AuthStrategy basic = AuthStrategyFactory.createBasicAuth("username", "password");
 * </code></pre>
 *
 * <h2>Bearer Token</h2>
 * <pre class="language-java"><code>
 * // Simple bearer token
 * AuthStrategy bearer = AuthStrategyFactory.createBearerToken("my-api-token");
 *
 * // Bearer token with custom refresh logic
 * AuthStrategy refreshable = AuthStrategyFactory.createBearerToken("initial-token", () -&gt; {
 *     // Custom refresh logic
 *     String newToken = fetchNewTokenFromServer();
 *     return newToken != null;
 * });
 * </code></pre>
 *
 * <h2>OAuth 2.0 Client Credentials</h2>
 * <pre class="language-java"><code>
 * AuthStrategy oauth = AuthStrategyFactory.createOAuth2ClientCredentials(
 *     "client-id",
 *     "client-secret",
 *     "https://auth.example.com/oauth/token"
 * );
 *
 * // With additional parameters (e.g., scope)
 * Map&lt;String, String&gt; params = new HashMap&lt;&gt;();
 * params.put("scope", "read write");
 * AuthStrategy oauthWithScope = AuthStrategyFactory.createOAuth2ClientCredentials(
 *     "client-id", "client-secret", "https://auth.example.com/oauth/token", params
 * );
 * </code></pre>
 *
 * <h2>OAuth 2.0 Password Grant</h2>
 * <pre class="language-java"><code>
 * AuthStrategy password = AuthStrategyFactory.createOAuth2Password(
 *     "user@example.com",
 *     "userPassword",
 *     "https://auth.example.com/oauth/token"
 * );
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see AuthStrategy
 * @see com.guinetik.rr.RocketRestConfig
 * @since 1.0.0
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