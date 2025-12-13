package com.guinetik.rr.http;

import org.junit.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RocketHeaders}.
 */
public class RocketHeadersTest {

    @Test
    public void testEmptyHeadersCreation() {
        RocketHeaders headers = new RocketHeaders();
        assertNotNull(headers.asMap());
        assertTrue(headers.asMap().isEmpty());
    }

    @Test
    public void testHeadersFromMap() {
        Map<String, String> map = new HashMap<>();
        map.put("X-Api-Key", "abc123");
        map.put("Accept", "application/json");

        RocketHeaders headers = new RocketHeaders(map);
        assertEquals("abc123", headers.get("X-Api-Key"));
        assertEquals("application/json", headers.get("Accept"));
    }

    @Test
    public void testSetAndGet() {
        RocketHeaders headers = new RocketHeaders()
                .set("X-Custom", "value1")
                .set("X-Another", "value2");

        assertEquals("value1", headers.get("X-Custom"));
        assertEquals("value2", headers.get("X-Another"));
    }

    @Test
    public void testContains() {
        RocketHeaders headers = new RocketHeaders()
                .set("X-Present", "yes");

        assertTrue(headers.contains("X-Present"));
        assertFalse(headers.contains("X-Missing"));
    }

    @Test
    public void testRemove() {
        RocketHeaders headers = new RocketHeaders()
                .set("X-ToRemove", "value")
                .set("X-ToKeep", "value");

        headers.remove("X-ToRemove");

        assertFalse(headers.contains("X-ToRemove"));
        assertTrue(headers.contains("X-ToKeep"));
    }

    @Test
    public void testContentType() {
        RocketHeaders headers = new RocketHeaders()
                .contentType(RocketHeaders.ContentTypes.APPLICATION_JSON);

        assertEquals("application/json", headers.get(RocketHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testAccept() {
        RocketHeaders headers = new RocketHeaders()
                .accept(RocketHeaders.ContentTypes.APPLICATION_JSON);

        assertEquals("application/json", headers.get(RocketHeaders.Names.ACCEPT));
    }

    @Test
    public void testBearerAuth() {
        RocketHeaders headers = new RocketHeaders()
                .bearerAuth("my-token-123");

        assertEquals("Bearer my-token-123", headers.get(RocketHeaders.Names.AUTHORIZATION));
    }

    @Test
    public void testBasicAuth() {
        RocketHeaders headers = new RocketHeaders()
                .basicAuth("user", "pass");

        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes());
        assertEquals(expectedAuth, headers.get(RocketHeaders.Names.AUTHORIZATION));
    }

    @Test
    public void testDefaultJson() {
        RocketHeaders headers = RocketHeaders.defaultJson();

        assertEquals(RocketHeaders.ContentTypes.APPLICATION_JSON, headers.get(RocketHeaders.Names.CONTENT_TYPE));
        assertEquals(RocketHeaders.ContentTypes.APPLICATION_JSON, headers.get(RocketHeaders.Names.ACCEPT));
    }

    @Test
    public void testMerge() {
        RocketHeaders base = new RocketHeaders()
                .set("X-Base", "base-value")
                .set("X-Override", "original");

        RocketHeaders override = new RocketHeaders()
                .set("X-Override", "new-value")
                .set("X-New", "new");

        RocketHeaders merged = base.merge(override);

        assertEquals("base-value", merged.get("X-Base"));
        assertEquals("new-value", merged.get("X-Override"));
        assertEquals("new", merged.get("X-New"));
    }

    @Test
    public void testMergeDoesNotModifyOriginal() {
        RocketHeaders original = new RocketHeaders().set("X-Original", "value");
        RocketHeaders other = new RocketHeaders().set("X-Other", "value");

        RocketHeaders merged = original.merge(other);

        // Original should not have X-Other
        assertFalse(original.contains("X-Other"));
        assertTrue(merged.contains("X-Other"));
    }

    @Test
    public void testAsMapIsUnmodifiable() {
        RocketHeaders headers = new RocketHeaders().set("X-Test", "value");
        Map<String, String> map = headers.asMap();

        try {
            map.put("X-New", "value");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testFluentChaining() {
        RocketHeaders headers = new RocketHeaders()
                .contentType(RocketHeaders.ContentTypes.APPLICATION_JSON)
                .accept(RocketHeaders.ContentTypes.APPLICATION_JSON)
                .bearerAuth("token")
                .set("X-Custom", "value");

        assertEquals(4, headers.asMap().size());
    }

    @Test
    public void testContentTypeConstants() {
        assertEquals("application/json", RocketHeaders.ContentTypes.APPLICATION_JSON);
        assertEquals("application/x-www-form-urlencoded", RocketHeaders.ContentTypes.APPLICATION_FORM);
        assertEquals("text/plain", RocketHeaders.ContentTypes.TEXT_PLAIN);
        assertEquals("multipart/form-data", RocketHeaders.ContentTypes.MULTIPART_FORM);
    }

    @Test
    public void testHeaderNameConstants() {
        assertEquals("Content-Type", RocketHeaders.Names.CONTENT_TYPE);
        assertEquals("Accept", RocketHeaders.Names.ACCEPT);
        assertEquals("Authorization", RocketHeaders.Names.AUTHORIZATION);
        assertEquals("User-Agent", RocketHeaders.Names.USER_AGENT);
        assertEquals("Content-Length", RocketHeaders.Names.CONTENT_LENGTH);
    }
}
