package com.guinetik.rr.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Pre-configured Jackson {@link ObjectMapper} with utility methods for JSON operations.
 *
 * <p>This class extends Jackson's ObjectMapper with RocketRest-specific configuration and
 * provides convenient static methods for common JSON serialization and deserialization tasks.
 *
 * <h2>Default Configuration</h2>
 * <ul>
 *   <li>Field visibility: ANY (serializes private fields)</li>
 *   <li>Getter/Setter visibility: NONE (ignores getters/setters)</li>
 *   <li>Indented output: enabled</li>
 *   <li>Unknown properties: ignored</li>
 *   <li>Null values: excluded from output</li>
 * </ul>
 *
 * <h2>Object Serialization</h2>
 * <pre class="language-java"><code>
 * User user = new User("John", "john@example.com");
 *
 * // To JSON string (indented)
 * String json = JsonObjectMapper.toJsonString(user);
 *
 * // To JSON string (compact)
 * String compact = JsonObjectMapper.toJsonStringNoIdent(user);
 * </code></pre>
 *
 * <h2>Object Deserialization</h2>
 * <pre class="language-java"><code>
 * String json = "{\"name\":\"John\",\"email\":\"john@example.com\"}";
 *
 * // To typed object
 * User user = JsonObjectMapper.jsonToObject(json, User.class);
 *
 * // To Map
 * Map&lt;String, Object&gt; map = JsonObjectMapper.jsonToMap(json);
 *
 * // To List of Maps
 * String arrayJson = "[{\"id\":1},{\"id\":2}]";
 * List&lt;Map&lt;String, Object&gt;&gt; list = JsonObjectMapper.jsonToListOfMap(arrayJson);
 * </code></pre>
 *
 * <h2>JsonNode Operations</h2>
 * <pre class="language-java"><code>
 * JsonNode node = JsonObjectMapper.getJsonNode(json);
 * String name = node.get("name").asText();
 *
 * Map&lt;String, Object&gt; map = JsonObjectMapper.jsonNodeToMap(node);
 * </code></pre>
 *
 * <h2>Singleton Access</h2>
 * <pre class="language-java"><code>
 * // Get pre-configured singleton instance
 * ObjectMapper mapper = JsonObjectMapper.get();
 *
 * // Get fresh default-configured mapper
 * ObjectMapper fresh = JsonObjectMapper.getDefault();
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see ObjectMapper
 * @since 1.0.0
 */
public class JsonObjectMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    private static class ObjectMapperInstanceHolder {
        static final ObjectMapper INSTANCE = getDefault();
    }

    public JsonObjectMapper() {
        setVisibilityChecker(
                getVisibilityChecker().withVisibility(PropertyAccessor.FIELD, Visibility.ANY)
                        .withVisibility(PropertyAccessor.GETTER, Visibility.NONE).withVisibility(
                                PropertyAccessor.SETTER, Visibility.NONE
                        ));
        enable(SerializationFeature.INDENT_OUTPUT);
    }

    private static String toJsonString(Object model, boolean ident) {
        ObjectMapper mapper = JsonObjectMapper.get();
        try {
            if (ident)
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
            //System.out.println(modelStr);
            return mapper.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JsonNode getJsonNode(String json) {
        try {
            return JsonObjectMapper.get().readValue(json, JsonNode.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Object> jsonToMap(String json) {
        return JsonObjectMapper.get().convertValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    public static Map<String, Object> jsonNodeToMap(JsonNode json) {
        return JsonObjectMapper.get().convertValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    public static <T> T jsonToObject(String json, Class<T> type) {

        try {
            return JsonObjectMapper.getDefault().readValue(json, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Map<String, Object>> jsonToListOfMap(String json) {
        ObjectMapper mapper = JsonObjectMapper.getDefault();
        try {
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toJsonString(Object model) {
        return toJsonString(model, true);
    }

    public static String toJsonStringNoIdent(Object model) {
        return toJsonString(model, false);
    }

    public static ObjectMapper getDefault() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //SerializationConfig config = mapper.getSerializationConfig();
        //config.withSerializationInclusion(Include.NON_EMPTY);
        //config.withSerializationInclusion(Include.NON_NULL);
        return mapper;
    }

    public static ObjectMapper get() {
        return ObjectMapperInstanceHolder.INSTANCE;
    }

    public static Map<String, Object> parseResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return get().readValue(response.toString(), new TypeReference<Map<String, Object>>() {});
        }
    }
}
