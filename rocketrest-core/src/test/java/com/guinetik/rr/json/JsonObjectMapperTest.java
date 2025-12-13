package com.guinetik.rr.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JsonObjectMapper}.
 */
public class JsonObjectMapperTest {

    @Test
    public void testGetSingleton() {
        ObjectMapper mapper1 = JsonObjectMapper.get();
        ObjectMapper mapper2 = JsonObjectMapper.get();

        assertNotNull(mapper1);
        assertSame(mapper1, mapper2);
    }

    @Test
    public void testGetDefault() {
        ObjectMapper mapper = JsonObjectMapper.getDefault();
        assertNotNull(mapper);
    }

    @Test
    public void testToJsonString() {
        TestUser user = new TestUser("John", "john@example.com");
        String json = JsonObjectMapper.toJsonString(user);

        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertTrue(json.contains("john@example.com"));
    }

    @Test
    public void testToJsonStringNoIndent() {
        TestUser user = new TestUser("Jane", "jane@example.com");
        String json = JsonObjectMapper.toJsonStringNoIdent(user);

        assertNotNull(json);
        assertTrue(json.contains("Jane"));
        // Compact format should have fewer newlines
    }

    @Test
    public void testJsonToObject() {
        String json = "{\"name\":\"Bob\",\"email\":\"bob@example.com\"}";
        TestUser user = JsonObjectMapper.jsonToObject(json, TestUser.class);

        assertNotNull(user);
        assertEquals("Bob", user.name);
        assertEquals("bob@example.com", user.email);
    }

    @Test
    public void testJsonToObjectWithUnknownProperties() {
        // JSON has extra field not in TestUser
        String json = "{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"extra\":\"ignored\"}";
        TestUser user = JsonObjectMapper.jsonToObject(json, TestUser.class);

        assertNotNull(user);
        assertEquals("Bob", user.name);
    }

    @Test
    public void testGetJsonNode() {
        String json = "{\"key\":\"value\",\"number\":42}";
        JsonNode node = JsonObjectMapper.getJsonNode(json);

        assertNotNull(node);
        assertEquals("value", node.get("key").asText());
        assertEquals(42, node.get("number").asInt());
    }

    @Test
    public void testJsonNodeToMap() {
        String json = "{\"name\":\"Test\",\"count\":10}";
        JsonNode node = JsonObjectMapper.getJsonNode(json);
        Map<String, Object> map = JsonObjectMapper.jsonNodeToMap(node);

        assertNotNull(map);
        assertEquals("Test", map.get("name"));
        assertEquals(10, map.get("count"));
    }

    @Test
    public void testJsonToListOfMap() {
        String json = "[{\"id\":1,\"name\":\"First\"},{\"id\":2,\"name\":\"Second\"}]";
        List<Map<String, Object>> list = JsonObjectMapper.jsonToListOfMap(json);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).get("id"));
        assertEquals("First", list.get(0).get("name"));
        assertEquals(2, list.get(1).get("id"));
        assertEquals("Second", list.get(1).get("name"));
    }

    @Test
    public void testJsonObjectMapperInstance() {
        JsonObjectMapper mapper = new JsonObjectMapper();
        assertNotNull(mapper);
    }

    @Test
    public void testNullHandling() {
        TestUser user = new TestUser("Name", null);
        String json = JsonObjectMapper.toJsonString(user);

        // Null values should be excluded based on configuration
        assertNotNull(json);
        assertTrue(json.contains("Name"));
    }

    @Test
    public void testInvalidJsonReturnsNull() {
        String invalidJson = "not valid json";
        JsonNode node = JsonObjectMapper.getJsonNode(invalidJson);

        // Should return null for invalid JSON
        assertNull(node);
    }

    @Test
    public void testEmptyJsonArray() {
        String json = "[]";
        List<Map<String, Object>> list = JsonObjectMapper.jsonToListOfMap(json);

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testEmptyJsonObject() {
        String json = "{}";
        JsonNode node = JsonObjectMapper.getJsonNode(json);

        assertNotNull(node);
        assertEquals(0, node.size());
    }

    // Helper class for testing
    public static class TestUser {
        public String name;
        public String email;

        public TestUser() {}

        public TestUser(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
