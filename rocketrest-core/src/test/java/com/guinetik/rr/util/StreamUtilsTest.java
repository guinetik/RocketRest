package com.guinetik.rr.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StreamUtils}.
 */
public class StreamUtilsTest {

    @Test
    public void testReadInputStreamAsString() throws IOException {
        String content = "Hello, World!";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = StreamUtils.readInputStreamAsString(is);

        assertEquals(content, result);
    }

    @Test
    public void testReadInputStreamAsStringWithMultiline() throws IOException {
        String content = "Line 1\nLine 2\nLine 3";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = StreamUtils.readInputStreamAsString(is);

        assertEquals(content, result);
    }

    @Test
    public void testReadInputStreamAsStringWithUnicode() throws IOException {
        String content = "Unicode: 日本語, Émoji: 🚀";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = StreamUtils.readInputStreamAsString(is);

        assertEquals(content, result);
    }

    @Test
    public void testReadInputStreamAsStringWithEmptyStream() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);

        String result = StreamUtils.readInputStreamAsString(is);

        assertEquals("", result);
    }

    @Test
    public void testReadInputStreamAsStringWithNull() throws IOException {
        String result = StreamUtils.readInputStreamAsString(null);

        assertEquals("", result);
    }

    @Test
    public void testReadErrorStream() {
        String errorContent = "Error message";
        InputStream errorStream = new ByteArrayInputStream(errorContent.getBytes(StandardCharsets.UTF_8));

        String result = StreamUtils.readErrorStream(errorStream);

        assertEquals(errorContent, result);
    }

    @Test
    public void testReadErrorStreamWithNull() {
        String result = StreamUtils.readErrorStream(null);

        assertNull(result);
    }

    @Test
    public void testReadErrorStreamWithEmpty() {
        InputStream errorStream = new ByteArrayInputStream(new byte[0]);

        String result = StreamUtils.readErrorStream(errorStream);

        assertEquals("", result);
    }

    @Test
    public void testReadInputStreamAsStringWithLargeContent() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Line ").append(i).append("\n");
        }
        String content = sb.toString();
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = StreamUtils.readInputStreamAsString(is);

        assertEquals(content, result);
    }

    @Test
    public void testReadInputStreamAsStringWithJson() throws IOException {
        String json = "{\"name\":\"John\",\"age\":30,\"active\":true}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        String result = StreamUtils.readInputStreamAsString(is);

        assertEquals(json, result);
    }
}
