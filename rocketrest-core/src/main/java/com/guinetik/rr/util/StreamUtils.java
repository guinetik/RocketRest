package com.guinetik.rr.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Utility class for stream operations to ensure proper resource management.
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