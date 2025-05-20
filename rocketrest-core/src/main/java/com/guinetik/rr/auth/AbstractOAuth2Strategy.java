package com.guinetik.rr.auth;

import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.http.DefaultHttpClient;
import com.guinetik.rr.http.RocketHeaders;
import com.guinetik.rr.http.RocketRestException;
import com.guinetik.rr.json.JsonObjectMapper;
import com.guinetik.rr.request.RequestBuilder;
import com.guinetik.rr.request.RequestSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for OAuth 2.0 authentication strategies.
 * Provides common functionality for different OAuth2 flows.
 */
public abstract class AbstractOAuth2Strategy implements AuthStrategy, RocketSSL.SSLAware {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, String> additionalParams;

    /**
     * OAuth 2.0 token endpoint URL to be used when refreshing credentials.
     */
    protected final String oauthTokenUrl;

    protected String accessToken;
    protected Date tokenExpiryTime;
    protected boolean isRefreshing;
    protected DefaultHttpClient httpClient;

    /**
     * Creates a new OAuth 2.0 strategy with additional parameters.
     *
     * @param tokenUrl        the OAuth 2.0 token endpoint URL
     * @param additionalParams additional parameters to include in the token request
     */
    protected AbstractOAuth2Strategy(String tokenUrl, Map<String, String> additionalParams) {
        this.oauthTokenUrl = tokenUrl;
        this.additionalParams = additionalParams != null ? additionalParams : new HashMap<>();
        this.isRefreshing = false;
    }

    @Override
    public RocketHeaders applyAuthHeaders(RocketHeaders headers) {
        if (accessToken != null && !accessToken.isEmpty()) {
            headers.bearerAuth(accessToken);
        }
        return headers;
    }

    @Override
    public boolean needsTokenRefresh() {
        if (accessToken == null || accessToken.isEmpty()) {
            return true;
        }
        if (tokenExpiryTime == null) {
            return true;
        }
        // Refresh the token if it's expired or about to expire in the next 5 minutes
        return Instant.now().plusSeconds(300).isAfter(tokenExpiryTime.toInstant());
    }

    /**
     * Helper method to perform POST requests.
     * 
     * @param url the URL to send the request to
     * @param formParams the form parameters to include in the request
     * @return the response body as a string
     * @throws IOException if an I/O error occurs
     */
    protected String post(String url, Map<String, String> formParams) throws IOException {
        // Initialize the HTTP client if not already done
        if (httpClient == null) {
            String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
            RocketRestOptions options = new RocketRestOptions();
            options.set(RocketRestOptions.LOGGING_ENABLED, true);
            options.set(RocketRestOptions.LOG_RAW_RESPONSE, true);
            options.set(RocketRestOptions.LOG_REQUEST_BODY, true);
            options.set(RocketRestOptions.LOG_RAW_RESPONSE, true);
            httpClient = new DefaultHttpClient(baseUrl, options);
        }
        // Build the form body
        StringBuilder formBody = new StringBuilder();
        try {
            boolean first = true;
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                if (!first) {
                    formBody.append("&");
                }
                first = false;
                formBody.append(entry.getKey())
                        .append("=")
                        .append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding form parameters", e);
            throw new TokenRefreshException("Error encoding form parameters", e);
        }
        // Create headers
        RocketHeaders headers = new RocketHeaders()
                .contentType(RocketHeaders.ContentTypes.APPLICATION_FORM);
        // Extract a path from the full URL for the request
        String endpoint = url.substring(url.lastIndexOf("/") + 1);
        // Create the request using a builder
        RequestSpec<String, String> requestSpec = RequestBuilder.<String, String>post(endpoint)
                .headers(headers)
                .body(formBody.toString())
                .responseType(String.class)
                .build();
        // Execute the request
        try {
            return httpClient.execute(requestSpec);
        } catch (RocketRestException e) {
            logger.error("Error executing POST request", e);
            logger.error("Status Code: {}", e.getStatusCode());
            logger.error("Response Body: {}", e.getResponseBody());
            throw e;
        }
        catch (Exception e) {
            logger.error("Error executing POST request", e);
            throw new IOException("Error executing POST request", e);
        }
    }

    @Override
    public boolean refreshCredentials() {
        if (isRefreshing) {
            logger.warn("Token refresh already in progress");
            return false;
        }
        // Check if the token URL is provided
        if (oauthTokenUrl == null || oauthTokenUrl.isEmpty()) {
            throw new TokenRefreshException("Token URL is required for OAuth2 flow");
        }
        // Validate the credentials
        validateCredentials();
        // Set the refreshing flag
        isRefreshing = true;
        // Try to refresh the token
        try {
            // Prepare form parameters
            Map<String, String> formParams = prepareTokenRequestParams();
            // Add any additional parameters
            formParams.putAll(additionalParams);
            // Execute POST request to get token
            String responseString = post(oauthTokenUrl, formParams);
            // Parse the response. Flexing some of the one-liners we pack with JsonObjectMapper
            Map<String, Object> tokenResponse = JsonObjectMapper.jsonNodeToMap(JsonObjectMapper.getJsonNode(responseString));
            // Process the token response
            return processTokenResponse(tokenResponse);
        } catch (Exception e) {
            logger.error("Error during token refresh", e);
            throw new TokenRefreshException("Error during token refresh", e);
        } finally {
            isRefreshing = false;
        }
    }

    /**
     * Validates that all required credentials are present.
     * @throws TokenRefreshException if any required credentials are missing
     */
    protected abstract void validateCredentials();

    /**
     * Prepares the parameters for the token request.
     * @return map of parameters to include in the token request
     */
    protected abstract Map<String, String> prepareTokenRequestParams();

    /**
     * Processes the token response and extracts relevant information.
     * @param tokenResponse the parsed token response
     * @return true if the token was successfully refreshed, false otherwise
     * @throws TokenRefreshException if there was an error processing the token response
     */
    protected boolean processTokenResponse(Map<String, Object> tokenResponse) {
        Object tokenObj = tokenResponse.get("access_token");
        Object expiresInObj = tokenResponse.get("expires_in");
        if (tokenObj != null) {
            accessToken = tokenObj.toString();
            // Calculate expiry time
            long expiresIn = 3600; // Default to 1 hour
            if (expiresInObj != null) {
                try {
                    expiresIn = Long.parseLong(expiresInObj.toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid expires_in value: {}", expiresInObj);
                }
            }
            tokenExpiryTime = Date.from(Instant.now().plusSeconds(expiresIn));
            logger.debug("Token refreshed successfully, expires in {} seconds", expiresIn);
            return true;
        } else {
            logger.error("Token response did not contain access_token: {}", tokenResponse);
            throw new TokenRefreshException("Token response did not contain access_token");
        }
    }

    /**
     * Gets the current access token.
     * @return the current access token, or null if not yet obtained
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Gets the token expiry time.
     * @return the token expiry time, or null if not yet obtained
     */
    public Date getTokenExpiryTime() {
        return tokenExpiryTime;
    }

    /**
     * Sets the SSL context for secure token requests.
     * @param sslContext the SSL context to use
     */
    public void configureSsl(SSLContext sslContext) {
        if (httpClient != null) {
            httpClient.configureSsl(sslContext);
        }
    }
} 