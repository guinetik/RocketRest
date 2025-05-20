package com.guinetik.rr.http;

/**
 * Exception thrown when a request is rejected because the circuit breaker is open.
 * This exception indicates that the service is considered unhealthy and requests are 
 * being fast-failed to prevent cascading failures.
 */
public class CircuitBreakerOpenException extends RocketRestException {
    
    private static final long serialVersionUID = 1L;
    private final long millisSinceLastFailure;
    private final long resetTimeoutMs;
    
    /**
     * Creates a new CircuitBreakerOpenException with the specified message.
     *
     * @param message The error message
     */
    public CircuitBreakerOpenException(String message) {
        this(message, 0, 0);
    }
    
    /**
     * Creates a new CircuitBreakerOpenException with the specified message 
     * and timing information about when the circuit might reset.
     *
     * @param message The error message
     * @param millisSinceLastFailure Milliseconds since the last failure
     * @param resetTimeoutMs Milliseconds until the circuit will attempt to reset
     */
    public CircuitBreakerOpenException(String message, long millisSinceLastFailure, long resetTimeoutMs) {
        super(message);
        this.millisSinceLastFailure = millisSinceLastFailure;
        this.resetTimeoutMs = resetTimeoutMs;
    }
    
    /**
     * Creates a new CircuitBreakerOpenException with the specified message, cause, 
     * and timing information about when the circuit might reset.
     *
     * @param message The error message
     * @param cause The cause of this exception
     * @param millisSinceLastFailure Milliseconds since the last failure
     * @param resetTimeoutMs Milliseconds until the circuit will attempt to reset
     */
    public CircuitBreakerOpenException(String message, Throwable cause, 
                                      long millisSinceLastFailure, long resetTimeoutMs) {
        super(message, cause);
        this.millisSinceLastFailure = millisSinceLastFailure;
        this.resetTimeoutMs = resetTimeoutMs;
    }
    
    /**
     * Gets the milliseconds since the last failure that caused the circuit to open.
     *
     * @return milliseconds since the last failure, or 0 if not available
     */
    public long getMillisSinceLastFailure() {
        return millisSinceLastFailure;
    }
    
    /**
     * Gets the configured timeout after which the circuit will try to reset.
     *
     * @return reset timeout in milliseconds, or 0 if not available
     */
    public long getResetTimeoutMs() {
        return resetTimeoutMs;
    }
    
    /**
     * Gets an estimated time in milliseconds until the circuit might reset.
     * A negative value indicates the circuit should have already attempted to reset.
     *
     * @return estimated milliseconds until reset, or 0 if timing data is unavailable
     */
    public long getEstimatedMillisUntilReset() {
        if (resetTimeoutMs > 0) {
            return resetTimeoutMs - millisSinceLastFailure;
        }
        return 0;
    }
    
    /**
     * Gets the underlying cause of the circuit opening.
     * This may be null if the circuit was already open when the request was made.
     *
     * @return the cause exception that led to the circuit opening, or null if not available
     */
    @Override
    public Throwable getCause() {
        return super.getCause();
    }
}
