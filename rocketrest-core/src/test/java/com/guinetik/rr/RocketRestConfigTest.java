package com.guinetik.rr;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for RocketRestConfig
 */
public class RocketRestConfigTest {
    
    @Test
    public void testConfigBuilder() {
        RocketRestConfig config = RocketRestConfig.builder("https://api.test.com").build();
        
        assertEquals("Service URL should be set correctly", 
                "https://api.test.com", config.getServiceUrl());
        assertNotNull("Default options should be initialized", config.getDefaultOptions());
    }
    
    @Test
    public void testDefaultOptions() {
        RocketRestConfig config = RocketRestConfig.builder("https://api.test.com")
                .defaultOption("timeout", 5000)
                .defaultOption("retries", 3)
                .build();
        
        assertEquals("Timeout option should be set", 
                5000, config.getDefaultOptions().getInt("timeout", 0));
        assertEquals("Retries option should be set", 
                3, config.getDefaultOptions().getInt("retries", 0));
    }
} 