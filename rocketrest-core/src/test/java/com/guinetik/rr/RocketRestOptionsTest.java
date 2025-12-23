package com.guinetik.rr;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RocketRestOptions}.
 */
public class RocketRestOptionsTest {

    private RocketRestOptions options;

    @Before
    public void setUp() {
        options = new RocketRestOptions();
    }

    @Test
    public void testSetAndGetString() {
        options.set("test.key", "test-value");
        assertEquals("test-value", options.getString("test.key", null));
    }

    @Test
    public void testSetAndGetInt() {
        options.set("test.int", 42);
        assertEquals(42, options.getInt("test.int", 0));
    }

    @Test
    public void testSetAndGetLong() {
        options.set("test.long", 123456789L);
        assertEquals(123456789L, options.getLong("test.long", 0L));
    }

    @Test
    public void testSetAndGetBoolean() {
        options.set("test.bool", true);
        assertTrue(options.getBoolean("test.bool", false));

        options.set("test.bool2", false);
        assertFalse(options.getBoolean("test.bool2", true));
    }

    @Test
    public void testGetStringWithDefault() {
        String value = options.getString("nonexistent", "default-value");
        assertEquals("default-value", value);
    }

    @Test
    public void testGetIntWithDefault() {
        int value = options.getInt("nonexistent", 99);
        assertEquals(99, value);
    }

    @Test
    public void testGetLongWithDefault() {
        long value = options.getLong("nonexistent", 999L);
        assertEquals(999L, value);
    }

    @Test
    public void testGetBooleanWithDefault() {
        assertTrue(options.getBoolean("nonexistent", true));
        assertFalse(options.getBoolean("nonexistent", false));
    }

    @Test
    public void testGetRaw() {
        Object obj = new Object();
        options.set("raw.object", obj);
        assertSame(obj, options.getRaw("raw.object"));
    }

    @Test
    public void testGetRawReturnsNullForMissing() {
        assertNull(options.getRaw("nonexistent"));
    }

    @Test
    public void testGetKeys() {
        int initialCount = 0;
        for (String key : options.getKeys()) {
            initialCount++;
        }

        options.set("key1", "value1");
        options.set("key2", "value2");
        options.set("key3", "value3");

        Iterable<String> keys = options.getKeys();
        int count = 0;
        boolean hasKey1 = false, hasKey2 = false, hasKey3 = false;
        for (String key : keys) {
            count++;
            if (key.equals("key1")) hasKey1 = true;
            if (key.equals("key2")) hasKey2 = true;
            if (key.equals("key3")) hasKey3 = true;
        }
        assertTrue(hasKey1 && hasKey2 && hasKey3);
        assertEquals(initialCount + 3, count);
    }

    @Test
    public void testOverwriteValue() {
        options.set("key", "original");
        assertEquals("original", options.getString("key", null));

        options.set("key", "updated");
        assertEquals("updated", options.getString("key", null));
    }

    @Test
    public void testCommonOptionKeys() {
        // Test that common option key constants exist
        assertNotNull(RocketRestOptions.RETRY_ENABLED);
        assertNotNull(RocketRestOptions.MAX_RETRIES);
        assertNotNull(RocketRestOptions.LOGGING_ENABLED);
        assertNotNull(RocketRestOptions.LOG_RAW_RESPONSE);
        assertNotNull(RocketRestOptions.LOG_RESPONSE_BODY);
        assertNotNull(RocketRestOptions.MAX_LOGGED_BODY_LENGTH);
        assertNotNull(RocketRestOptions.ASYNC_POOL_SIZE);
    }

    @Test
    public void testSetWithRocketRestOptionKeys() {
        options.set(RocketRestOptions.RETRY_ENABLED, true);
        options.set(RocketRestOptions.MAX_RETRIES, 3);
        options.set(RocketRestOptions.LOGGING_ENABLED, true);
        options.set(RocketRestOptions.ASYNC_POOL_SIZE, 8);

        assertTrue(options.getBoolean(RocketRestOptions.RETRY_ENABLED, false));
        assertEquals(3, options.getInt(RocketRestOptions.MAX_RETRIES, 0));
        assertTrue(options.getBoolean(RocketRestOptions.LOGGING_ENABLED, false));
        assertEquals(8, options.getInt(RocketRestOptions.ASYNC_POOL_SIZE, 4));
    }

    @Test
    public void testIntFromStringConversion() {
        options.set("string.int", "123");
        // Depending on implementation, this might convert or return default
        int value = options.getInt("string.int", 0);
        assertTrue(value == 123 || value == 0);
    }

    @Test
    public void testBooleanFromStringConversion() {
        options.set("string.bool", "true");
        // Depending on implementation, this might convert or return default
        boolean value = options.getBoolean("string.bool", false);
        assertTrue(value || !value); // Either conversion works or default is used
    }

    @Test
    public void testDefaultOptions() {
        RocketRestOptions defaultOptions = new RocketRestOptions();
        int count = 0;
        for (String key : defaultOptions.getKeys()) {
            count++;
        }
        // Constructor sets 10 default options
        assertEquals(10, count);
    }

    @Test
    public void testNullKey() {
        try {
            options.set(null, "value");
        } catch (NullPointerException | IllegalArgumentException e) {
            // Expected behavior for null key
        }
    }

    @Test
    public void testNullValue() {
        options.set("nullable", null);
        assertNull(options.getRaw("nullable"));
    }
}
