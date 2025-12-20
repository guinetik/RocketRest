package com.guinetik.rr.auth;

import com.guinetik.rr.http.HttpConstants;
import com.guinetik.rr.http.RocketRestException;

/**
 * Exception thrown when an API request fails due to token expiration.
 *
 * <p>This exception is thrown when a request receives a 401 Unauthorized response,
 * indicating that the authentication token has expired or is invalid. The retry
 * mechanism in {@link com.guinetik.rr.api.AbstractApiClient} catches this exception
 * and attempts to refresh the token before retrying.
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * RuntimeException
 *   └── RocketRestException
 *         └── TokenExpiredException
 * </pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketRestException
 * @see com.guinetik.rr.auth.AuthStrategy
 * @since 1.0.0
 */
public class TokenExpiredException extends RocketRestException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new TokenExpiredException with the specified message.
     *
     * @param message The exception message
     */
    public TokenExpiredException(String message) {
        super(message, HttpConstants.StatusCodes.UNAUTHORIZED, null);
    }

    /**
     * Creates a new TokenExpiredException with the specified message and cause.
     *
     * @param message The exception message
     * @param cause The underlying cause
     */
    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
} 