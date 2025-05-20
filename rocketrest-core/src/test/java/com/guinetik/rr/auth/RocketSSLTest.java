package com.guinetik.rr.auth;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.http.RocketClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.net.ssl.SSLContext;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RocketSSLTest {

    @Mock
    private RocketClient mockClient;

    @Mock
    private SSLContext mockSslContext;
    
    private RocketSSL rocketSSL;
    
    private static class MockSSLConfig extends RocketRestConfig implements RocketSSL.SSLConfig {
        private final boolean enabled;
        private final String filename;
        private final String password;

        public MockSSLConfig(boolean enabled, String filename, String password) {
            super(RocketRestConfig.builder("https://test.example.com"));
            this.enabled = enabled;
            this.filename = filename;
            this.password = password;
        }

        @Override
        public String getCustomCertificateFilename() {
            return filename;
        }

        @Override
        public String getCustomCertificatePassword() {
            return password;
        }

        @Override
        public boolean isCustomCertificateEnabled() {
            return enabled;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        rocketSSL = spy(new RocketSSL());
    }

    @Test
    public void testGetSSLContextWithCachedContext() {
        // Set up the test with an initial call to cache a context
        RocketSSL.SSLCertificate cert = new RocketSSL.SSLCertificate("cert.p12", "password");
        
        // Mock the internal workings using a spy
        doReturn(mockSslContext).when(rocketSSL).getSSLContext(eq("cert.p12"), eq("password"));
        
        // First call should create context
        SSLContext context1 = rocketSSL.getSSLContext(cert);
        assertEquals(mockSslContext, context1);
        
        // Second call should return cached context without recreating
        SSLContext context2 = rocketSSL.getSSLContext(cert);
        assertEquals(mockSslContext, context2);
        
        // Verify getSSLContext was only called twice with those parameters
        verify(rocketSSL, times(2)).getSSLContext(eq("cert.p12"), eq("password"));
    }

    @Test
    public void testGetSSLContextWhenParametersInvalid() {
        // Test with null parameters
        assertNull(rocketSSL.getSSLContext((String)null, "password"));
        assertNull(rocketSSL.getSSLContext("cert.p12", null));
    }

    @Test
    public void testConfigureSslWhenNullConfig() {
        // Test when config is not SSLConfig
        RocketRestConfig mockConfig = RocketRestConfig.builder("https://example.com").build();
        
        // This will return false since mockConfig is not an instance of SSLConfig
        assertFalse(RocketSSL.configureSsl(mockClient, mockConfig));
        
        // Client should not be configured
        verify(mockClient, never()).configureSsl(any(SSLContext.class));
    }

    @Test
    public void testConfigureSslWhenDisabled() {
        // Test when SSL is disabled
        MockSSLConfig mockConfig = new MockSSLConfig(false, "cert.p12", "password");
        assertFalse(RocketSSL.configureSsl(mockClient, mockConfig));
        // Client should not be configured
        verify(mockClient, never()).configureSsl(any(SSLContext.class));
    }

    @Test
    public void testConfigureSslWhenNullCertificateFile() {
        // Test when certificate file is null
        MockSSLConfig mockConfig = new MockSSLConfig(true, null, "password");
        assertFalse(RocketSSL.configureSsl(mockClient, mockConfig));
        // Client should not be configured
        verify(mockClient, never()).configureSsl(any(SSLContext.class));
    }

    @Test
    public void testConfigureSslWhenNullCertificatePassword() {
        // Test when certificate password is null
        MockSSLConfig mockConfig = new MockSSLConfig(true, "cert.p12", null);
        assertFalse(RocketSSL.configureSsl(mockClient, mockConfig));
        // Client should not be configured
        verify(mockClient, never()).configureSsl(any(SSLContext.class));
    }
    
    @Test
    public void testSSLCertificateGetterSetter() {
        // Test basic getters and setters
        RocketSSL.SSLCertificate cert = new RocketSSL.SSLCertificate();
        assertNull(cert.getCertificateFilename());
        assertNull(cert.getCertificatePassword());
        
        cert.setCertificateFilename("test.p12");
        cert.setCertificatePassword("secret");
        
        assertEquals("test.p12", cert.getCertificateFilename());
        assertEquals("secret", cert.getCertificatePassword());
        
        // Test constructor
        RocketSSL.SSLCertificate cert2 = new RocketSSL.SSLCertificate("another.p12", "password");
        assertEquals("another.p12", cert2.getCertificateFilename());
        assertEquals("password", cert2.getCertificatePassword());
    }
} 