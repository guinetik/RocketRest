package com.guinetik.rr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration options for RocketRest API clients.
 *
 * <p>This class provides a type-safe, centralized container for all configurable options
 * across different client implementations. Options are stored as key-value pairs and
 * can be retrieved with type-specific methods.
 *
 * <h2>Available Options</h2>
 * <table border="1">
 *   <tr><th>Option</th><th>Type</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>{@link #RETRY_ENABLED}</td><td>Boolean</td><td>true</td><td>Enable automatic retry on failure</td></tr>
 *   <tr><td>{@link #MAX_RETRIES}</td><td>Integer</td><td>3</td><td>Maximum number of retry attempts</td></tr>
 *   <tr><td>{@link #RETRY_DELAY}</td><td>Long</td><td>1000</td><td>Delay between retries in milliseconds</td></tr>
 *   <tr><td>{@link #LOGGING_ENABLED}</td><td>Boolean</td><td>true</td><td>Enable request/response logging</td></tr>
 *   <tr><td>{@link #LOG_REQUEST_BODY}</td><td>Boolean</td><td>false</td><td>Log request body content</td></tr>
 *   <tr><td>{@link #LOG_RESPONSE_BODY}</td><td>Boolean</td><td>false</td><td>Log response body content</td></tr>
 *   <tr><td>{@link #ASYNC_POOL_SIZE}</td><td>Integer</td><td>4</td><td>Thread pool size for async operations</td></tr>
 * </table>
 *
 * <h2>Usage</h2>
 * <pre class="language-java"><code>
 * // Configure via RocketRestConfig builder
 * RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
 *     .defaultOptions(options -&gt; {
 *         options.set(RocketRestOptions.RETRY_ENABLED, true);
 *         options.set(RocketRestOptions.MAX_RETRIES, 5);
 *         options.set(RocketRestOptions.LOG_REQUEST_BODY, true);
 *     })
 *     .build();
 *
 * // Or configure directly on client
 * RocketRest client = new RocketRest(config);
 * client.configure(RocketRestOptions.RETRY_DELAY, 2000L);
 * </code></pre>
 *
 * <h2>Reading Options</h2>
 * <pre class="language-java"><code>
 * RocketRestOptions options = new RocketRestOptions();
 * boolean retryEnabled = options.getBoolean(RocketRestOptions.RETRY_ENABLED, false);
 * int maxRetries = options.getInt(RocketRestOptions.MAX_RETRIES, 3);
 * long delay = options.getLong(RocketRestOptions.RETRY_DELAY, 1000L);
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RocketRestConfig
 * @see RocketRest#configure(String, Object)
 * @since 1.0.0
 */
public class RocketRestOptions {
    // Common options
    public static final String RETRY_ENABLED = "retry.enabled";
    public static final String MAX_RETRIES = "retry.max";
    public static final String RETRY_DELAY = "retry.delay";
    public static final String LOGGING_ENABLED = "logging.enabled";
    public static final String TIMING_ENABLED = "timing.enabled";
    public static final String LOG_REQUEST_BODY = "logging.request.body";
    public static final String LOG_RESPONSE_BODY = "logging.response.body";
    public static final String LOG_RAW_RESPONSE = "logging.response.raw";
    public static final String MAX_LOGGED_BODY_LENGTH = "logging.body.maxlength";

    // Async options
    public static final String ASYNC_POOL_SIZE = "async.pool.size";

    // Token refresh URL option
    public static final String TOKEN_URL = "tokenUrl";

    private final Map<String, Object> options = new HashMap<>();

    /**
     * Creates a new ClientOptions with default values.
     */
    public RocketRestOptions() {
        // Set defaults
        options.put(RETRY_ENABLED, Boolean.TRUE);
        options.put(MAX_RETRIES, 3);
        options.put(RETRY_DELAY, 1000L);
        options.put(LOGGING_ENABLED, Boolean.TRUE);
        options.put(TIMING_ENABLED, Boolean.TRUE);
        options.put(LOG_REQUEST_BODY, Boolean.FALSE);
        options.put(LOG_RESPONSE_BODY, Boolean.FALSE);
        options.put(LOG_RAW_RESPONSE, Boolean.TRUE);
        options.put(MAX_LOGGED_BODY_LENGTH, 4000);
        options.put(ASYNC_POOL_SIZE, 4);
    }

    /**
     * Sets an option value.
     *
     * @param key   The option key.
     * @param value The option value.
     * @return This option instance for method chaining.
     */
    public RocketRestOptions set(String key, Object value) {
        options.put(key, value);
        return this;
    }

    /**
     * Gets all option keys.
     *
     * @return Set of all option keys.
     */
    public Set<String> getKeys() {
        return options.keySet();
    }

    /**
     * Gets the raw option value without type casting.
     *
     * @param key The option key.
     * @return The raw option value, or null if not set.
     */
    public Object getRaw(String key) {
        return options.get(key);
    }

    /**
     * Gets an option value.
     *
     * @param <T>          The expected type of the option value.
     * @param key          The option key.
     * @param defaultValue The default value to return if the option is not set.
     * @return The option value, or the default value if not set.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = options.get(key);
        if (defaultValue != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * Gets a boolean option value.
     *
     * @param key          The option key.
     * @param defaultValue The default value to return if the option is not set.
     * @return The boolean option value.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = options.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Gets an integer option value.
     *
     * @param key          The option key.
     * @param defaultValue The default value to return if the option is not set.
     * @return The integer option value.
     */
    public int getInt(String key, int defaultValue) {
        Object value = options.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Gets a long option value.
     *
     * @param key          The option key.
     * @param defaultValue The default value to return if the option is not set.
     * @return The long option value.
     */
    public long getLong(String key, long defaultValue) {
        Object value = options.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    public boolean contains(String feature) {
        return this.options.containsKey(feature);
    }

    public String getString(String feature, String defaultValue) {
        Object value = options.get(feature);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
}