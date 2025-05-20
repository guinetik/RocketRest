package com.guinetik.rr.http;

import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.auth.TokenExpiredException;
import com.guinetik.rr.json.JsonObjectMapper;
import com.guinetik.rr.request.RequestSpec;
import com.guinetik.rr.util.ResponseLogger;
import com.guinetik.rr.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Default implementation of HttpRequestClient using HttpURLConnection.
 * This class handles HTTP requests without external dependencies.
 */
public class DefaultHttpClient implements RocketClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHttpClient.class);

    private String baseUrl;
    private final RocketRestOptions clientOptions;
    private SSLContext sslContext;

    /**
     * Creates a new DefaultHttpClient with the specified base URL.
     *
     * @param baseUrl The base URL for all requests
     */
    public DefaultHttpClient(String baseUrl) {
        this(baseUrl, new RocketRestOptions());
    }

    /**
     * Creates a new DefaultHttpClient with the specified base URL and client options.
     *
     * @param baseUrl       The base URL for all requests
     * @param clientOptions The client options
     */
    public DefaultHttpClient(String baseUrl, RocketRestOptions clientOptions) {
        this.baseUrl = baseUrl.endsWith(HttpConstants.Url.PATH_SEPARATOR) ?
                baseUrl : baseUrl + HttpConstants.Url.PATH_SEPARATOR;
        this.clientOptions = clientOptions != null ? clientOptions : new RocketRestOptions();
    }

    @Override
    public void configureSsl(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Gets the client options.
     *
     * @return The client options
     */
    public RocketRestOptions getClientOptions() {
        return clientOptions;
    }

    @Override
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
        try {
            String fullUrl = buildFullUrl(requestSpec);
            HttpURLConnection connection = configureConnection(fullUrl, requestSpec);
            setRequestBody(connection, requestSpec);

            return executeRequest(connection, requestSpec);
        } catch (TokenExpiredException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof RocketRestException) {
                throw (RocketRestException) e;
            }
            throw new RocketRestException(HttpConstants.Errors.EXECUTE_REQUEST, e);
        }
    }

    /**
     * Builds the full URL including endpoint and query parameters.
     * Validates that absolute URLs are not used with a non-empty base URL.
     *
     * @throws RocketRestException if an absolute URL is used with a non-empty base URL
     */
    private String buildFullUrl(RequestSpec<?, ?> requestSpec) throws RocketRestException {
        String endpoint = requestSpec.getEndpoint();

        // Check if the endpoint is an absolute URL
        boolean isAbsoluteUrl = endpoint.startsWith("http://") || endpoint.startsWith("https://");

        // If baseUrl is not empty/blank and the endpoint is absolute, throw exception
        if (isAbsoluteUrl && !baseUrl.trim().isEmpty() && !baseUrl.equals("/")) {
            throw new RocketRestException(
                "Cannot use absolute URL '" + endpoint + "' with base URL '" + baseUrl +
                "'. Either use a relative path or set base URL to empty string.",
                400,
                null);
        }

        // If the endpoint is absolute, use it directly
        if (isAbsoluteUrl) {
            String fullUrl = endpoint;
            if (!requestSpec.getQueryParams().isEmpty()) {
                fullUrl += buildQueryString(requestSpec.getQueryParams());
            }
            return fullUrl;
        }

        // Handle relative endpoints
        if (endpoint.startsWith(HttpConstants.Url.PATH_SEPARATOR)) {
            endpoint = endpoint.substring(1);
        }

        String fullUrl = baseUrl + endpoint;

        if (!requestSpec.getQueryParams().isEmpty()) {
            fullUrl += buildQueryString(requestSpec.getQueryParams());
        }

        return fullUrl;
    }

    /**
     * Configures the HttpURLConnection with proper settings.
     */
    private <Req, Res> HttpURLConnection configureConnection(String fullUrl, RequestSpec<Req, Res> requestSpec)
            throws IOException {
        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // Configure SSL if needed
        if (connection instanceof HttpsURLConnection && sslContext != null) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
        }
        // Set timeouts
        connection.setConnectTimeout(HttpConstants.Timeouts.DEFAULT_CONNECT_TIMEOUT);
        connection.setReadTimeout(HttpConstants.Timeouts.DEFAULT_READ_TIMEOUT);
        // Configure method
        connection.setRequestMethod(requestSpec.getMethod());
        // Set headers
        setRequestHeaders(connection, requestSpec);
        return connection;
    }

    /**
     * Sets the request headers on the connection.
     */
    private <Req, Res> void setRequestHeaders(HttpURLConnection connection, RequestSpec<Req, Res> requestSpec) {
        RocketHeaders headers = requestSpec.getHeaders();
        // Set all headers on the connection
        headers.asMap().forEach(connection::setRequestProperty);
    }

    /**
     * Sets the request body if applicable.
     */
    private <Req, Res> void setRequestBody(HttpURLConnection connection, RequestSpec<Req, Res> requestSpec)
            throws IOException {
        boolean hasBody = requestSpec.getBody() != null && isMethodWithBody(requestSpec.getMethod());

        if (hasBody) {
            connection.setDoOutput(true);
            String jsonBody;

            // If the body is already a String, use it directly
            if (requestSpec.getBody() instanceof String) {
                jsonBody = (String) requestSpec.getBody();
            } else {
                // Otherwise convert to JSON
                jsonBody = JsonObjectMapper.toJsonString(requestSpec.getBody());
            }

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
    }

    /**
     * Checks if the HTTP method supports a request body.
     */
    private boolean isMethodWithBody(String method) {
        return method.equals(HttpConstants.Methods.POST) ||
                method.equals(HttpConstants.Methods.PUT) ||
                method.equals(HttpConstants.Methods.PATCH);
    }

    /**
     * Executes the configured request and processes the response.
     */
    private <Req, Res> Res executeRequest(HttpURLConnection connection, RequestSpec<Req, Res> requestSpec)
            throws IOException, RocketRestException {
        if (getClientOptions().getBoolean(RocketRestOptions.LOGGING_ENABLED, true)) {
            logger.debug("Executing request: {} {}", connection.getRequestMethod(), connection.getURL());
        }
        int statusCode = connection.getResponseCode();
        Map<String, String> responseHeaders = extractResponseHeaders(connection);
        // Log raw response
        ResponseLogger.logRawResponse(statusCode, responseHeaders, clientOptions);

        // Check for token expired
        if (statusCode == HttpConstants.StatusCodes.UNAUTHORIZED) {
            throw new TokenExpiredException(HttpConstants.Errors.TOKEN_EXPIRED);
        }

        // Handle response based on status code
        if (statusCode >= HttpConstants.StatusCodes.SUCCESS_MIN &&
                statusCode < HttpConstants.StatusCodes.SUCCESS_MAX) {
            return handleSuccessResponse(connection, requestSpec);
        } else {
            handleErrorResponse(connection, statusCode);
            // This line is never reached as handleErrorResponse always throws an exception
            return null;
        }
    }

    /**
     * Extracts response headers from the connection.
     */
    private Map<String, String> extractResponseHeaders(HttpURLConnection connection) {
        Map<String, String> responseHeaders = new HashMap<>();
        // Extract all headers
        for (int i = 0; ; i++) {
            String headerName = connection.getHeaderFieldKey(i);
            String headerValue = connection.getHeaderField(i);
            if (headerName == null && headerValue == null) {
                break;
            }
            if (headerName != null) {
                responseHeaders.put(headerName, headerValue);
            }
        }
        return responseHeaders;
    }

    /**
     * Handles a successful HTTP response.
     */
    private <Req, Res> Res handleSuccessResponse(HttpURLConnection connection, RequestSpec<Req, Res> requestSpec)
            throws IOException {

        // Handle void response
        if (requestSpec.getResponseType() == Void.class) {
            return null;
        }

        // Read and process response body
        try (InputStream is = connection.getInputStream()) {
            String responseString = StreamUtils.readInputStreamAsString(is);

            // Log response body if enabled
            ResponseLogger.logResponseBody(responseString, clientOptions);

            // Special case for String.class - return the raw response string
            if (requestSpec.getResponseType() == String.class) {
                @SuppressWarnings("unchecked")
                Res result = (Res) responseString;
                return result;
            }

            // Parse response to the requested type
            return JsonObjectMapper.jsonToObject(responseString, requestSpec.getResponseType());
        }
    }

    /**
     * Handles an HTTP error response.
     * Always throws an exception with the error details.
     *
     * @throws RocketRestException Always thrown with error details
     */
    private void handleErrorResponse(HttpURLConnection connection, int statusCode) throws RocketRestException {
        // Get error details from the error stream
        String errorBody = Optional.ofNullable(connection.getErrorStream())
                .map(is -> {
                    try {
                        return StreamUtils.readInputStreamAsString(is);
                    } catch (IOException e) {
                        logger.warn("Error reading error stream", e);
                        return null;
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // Ignore close errors
                        }
                    }
                })
                .orElse(null);

        // Log error response body if enabled
        ResponseLogger.logResponseBody(errorBody, this.getClientOptions());

        throw new RocketRestException(
                HttpConstants.Errors.REQUEST_FAILED + statusCode,
                statusCode,
                errorBody
        );
    }

    /**
     * Builds a query string from a map of parameters.
     *
     * @param params The query parameters
     * @return The formatted query string
     */
    private String buildQueryString(Map<String, String> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringJoiner sj = new StringJoiner(
                HttpConstants.Url.QUERY_SEPARATOR,
                HttpConstants.Url.QUERY_PREFIX,
                "");

        params.forEach((key, value) ->
                sj.add(key + HttpConstants.Url.PARAM_EQUALS + encodeParam(value))
        );
        return sj.toString();
    }

    /**
     * Encodes a URL parameter.
     *
     * @param param The parameter to encode
     * @return The encoded parameter
     */
    private String encodeParam(String param) {
        try {
            return java.net.URLEncoder.encode(param, HttpConstants.Encoding.UTF8);
        } catch (Exception e) {
            logger.warn(HttpConstants.Errors.ENCODE_PARAM, param, e);
            return param;
        }
    }
} 