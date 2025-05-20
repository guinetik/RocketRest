package com.guinetik.examples;

/**
 * Interface for all RocketRest examples
 */
public interface Example {
    /**
     * Get the name of the example to display in the menu
     * @return The example name
     */
    String getName();
    
    /**
     * Run the example
     */
    void run();
} 