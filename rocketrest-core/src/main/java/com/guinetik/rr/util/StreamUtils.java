package com.guinetik.rr.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Utility class for safe input stream operations with proper resource management.
 *
 * <p>This class provides static methods for reading input streams into strings,
 * ensuring proper cleanup of resources even when exceptions occur.
 *
 * <h2>Reading Input Streams</h2>
 * <pre class="language-java"><code>
 * // Read an input stream to string
 * InputStream is = connection.getInputStream();
 * String content = StreamUtils.readInputStreamAsString(is);
 *
 * // Read error stream (returns null if unavailable)
 * InputStream errorStream = connection.getErrorStream();
 * String error = StreamUtils.readErrorStream(errorStream);
 * if (error != null) {
 *     System.err.println("Error response: " + error);
 * }
 * </code></pre>
 *
 * <h2>Usage in HTTP Response Handling</h2>
 * <pre class="language-java"><code>
 * HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 * int status = conn.getResponseCode();
 *
 * if (status &gt;= 200 &amp;&amp; status &lt; 300) {
 *     String body = StreamUtils.readInputStreamAsString(conn.getInputStream());
 *     // Process successful response
 * } else {
 *     String error = StreamUtils.readErrorStream(conn.getErrorStream());
 *     // Handle error response
 * }
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @since 1.0.0
 */
public final class StreamUtils {
    
    private StreamUtils() {
        // Utility class, no instantiation
    }
    
    /**
     * Reads an input stream into a string, handling resource cleanup properly.
     * This method ensures the Scanner is properly closed after reading.
     *
     * @param is The input stream to read
     * @return The string contents of the stream
     * @throws IOException If an I/O error occurs
     */
    public static String readInputStreamAsString(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        
        try (Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
    
    /**
     * Attempts to read from an error stream, handling possible exceptions gracefully.
     * This method ensures the Scanner is properly closed after reading.
     *
     * @param errorStream The error stream to read
     * @return The string contents of the stream, or null if not available
     */
    public static String readErrorStream(InputStream errorStream) {
        if (errorStream == null) {
            return null;
        }
        
        try (Scanner scanner = new Scanner(errorStream, "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            // Silently handle errors reading the error stream
            return null;
        }
    }
} 