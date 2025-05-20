package com.guinetik.rr.auth;

import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.http.RocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling SSL (TLS 1.2 and 1.3) certificates and contexts.
 * Used for setting up secure connections with custom certificates.
 */
public class RocketSSL {

    private static final Logger logger = LoggerFactory.getLogger(RocketSSL.class);

    /**
     * Interface for objects that can provide certificate information.
     */
    public interface SSLConfig {
        String getCustomCertificateFilename();

        String getCustomCertificatePassword();

        boolean isCustomCertificateEnabled();
    }

    /**
     * Interface for authentication strategies that can be configured with an SSL context.
     * This interface is implemented by strategies that need to make HTTPS requests,
     * such as OAuth2 strategies that need to get tokens from a server.
     */
    public interface SSLAware {

        /**
         * Sets the SSL context to be used for HTTPS requests made by the strategy.
         *
         * @param sslContext the SSL context to use
         */
        void configureSsl(SSLContext sslContext);
    }

    /**
     * Class representing certificate information.
     */
    public static class SSLCertificate {
        private String certificateFilename;
        private String certificatePassword;

        public SSLCertificate(String certificateFilename, String certificatePassword) {
            this.certificateFilename = certificateFilename;
            this.certificatePassword = certificatePassword;
        }

        public SSLCertificate() {
        }

        public String getCertificateFilename() {
            return certificateFilename;
        }

        public void setCertificateFilename(String certificateFilename) {
            this.certificateFilename = certificateFilename;
        }

        public String getCertificatePassword() {
            return certificatePassword;
        }

        public void setCertificatePassword(String certificatePassword) {
            this.certificatePassword = certificatePassword;
        }
    }

    private Map<String, SSLContext> sslContexts = new HashMap<>();

    /**
     * Gets the SSL context for the given certificate information.
     *
     * @param SSLCertificate the certificate information
     * @return the SSL context, or null if an error occurred
     */
    public synchronized SSLContext getSSLContext(SSLCertificate SSLCertificate) {
        return getSSLContext(SSLCertificate.getCertificateFilename(), SSLCertificate.getCertificatePassword());
    }

    /**
     * Gets the SSL context for the given certificate file and password.
     *
     * @param fileName the certificate file path
     * @param certPass the certificate password
     * @return the SSL context, or null if an error occurred
     */
    public synchronized SSLContext getSSLContext(String fileName, String certPass) {
        SSLContext sc;
        if (sslContexts.get(fileName) != null)
            return sslContexts.get(fileName);
        try {
            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            if (fileName == null || certPass == null) {
                throw new IllegalArgumentException("Security certificate file name or password not found.");
            }
            KeyManagerFactory kmf;

            kmf = KeyManagerFactory.getInstance("SunX509");

            InputStream fis = null;
            try {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                fis = Files.newInputStream(Paths.get(fileName));
                ks.load(fis, certPass.toCharArray());
                kmf.init(ks, certPass.toCharArray());
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            try {
                // Try TLS 1.3 first
                sc = SSLContext.getInstance("TLSv1.3");
            } catch (NoSuchAlgorithmException e) {
                // Fall back to TLS 1.2 if 1.3 is not available
                logger.info("TLS 1.3 not available, falling back to TLS 1.2");
                sc = SSLContext.getInstance("TLSv1.2");
            }
            sc.init(kmf.getKeyManagers(), new TrustManager[]{tm}, null);
            sslContexts.put(fileName, sc);
        } catch (Exception e) {
            logger.error("Exception occurred getting SSL context: {}", e.getMessage());
            return null;
        }

        return sc;
    }

    /**
     * Configures SSL for a client if the provided config implements SSLAware and has a custom
     * certificate enabled.
     *
     * @param client The client to configure
     * @param config The configuration that might contain SSL settings
     * @return true if SSL was successfully configured, false otherwise
     */
    public static boolean configureSsl(RocketClient client, RocketRestConfig config) {
        if (!(config instanceof SSLConfig)) {
            return false;
        }

        SSLConfig certConfig = (SSLConfig) config;

        if (!certConfig.isCustomCertificateEnabled()) {
            return false;
        }

        String certFile = certConfig.getCustomCertificateFilename();
        String certPass = certConfig.getCustomCertificatePassword();

        if (certFile == null || certPass == null) {
            logger.warn("Certificate file or password is null");
            return false;
        }

        logger.info("Configuring SSL with custom certificate: {}", certFile);

        SSLCertificate cert = new SSLCertificate(certFile, certPass);
        RocketSSL ssl = new RocketSSL();
        SSLContext sslContext = ssl.getSSLContext(cert);

        if (sslContext == null) {
            logger.error("Failed to configure SSL context with certificate: {}", certFile);
            return false;
        }

        // Configure HTTP client
        client.configureSsl(sslContext);

        // Configure auth strategy if it supports SSL
        if (config.getAuthStrategy() instanceof SSLAware) {
            ((SSLAware) config.getAuthStrategy()).configureSsl(sslContext);
            logger.info("SSL context configured for authentication strategy");
        }

        logger.info("SSL context configured successfully");
        return true;
    }
}