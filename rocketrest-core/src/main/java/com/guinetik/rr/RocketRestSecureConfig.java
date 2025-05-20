package com.guinetik.rr;

import com.guinetik.rr.auth.AuthStrategy;
import com.guinetik.rr.auth.RocketSSL;

import java.util.function.Consumer;

public class RocketRestSecureConfig extends RocketRestConfig implements RocketSSL.SSLConfig {
    /**
     * Builder for creating RocketRestSecureConfig instances.
     */
    public static class SecureBuilder extends RocketRestConfig.Builder {
        private boolean customCertificateEnabled;
        private String customCertificateFilename;
        private String customCertificatePassword;

        protected SecureBuilder(String serviceUrl) {
            super(serviceUrl);
        }

        /**
         * Enables a custom SSL certificate with the given filename and password.
         *
         * @param filename the custom certificate filename
         * @param password the custom certificate password
         * @return this builder instance
         */
        public SecureBuilder withCustomCertificate(String filename, String password) {
            this.customCertificateEnabled = true;
            this.customCertificateFilename = filename;
            this.customCertificatePassword = password;
            return this;
        }

        @Override
        public SecureBuilder tokenUrl(String tokenUrl) {
            super.tokenUrl(tokenUrl);
            return this;
        }

        @Override
        public SecureBuilder authStrategy(AuthStrategy authStrategy) {
            super.authStrategy(authStrategy);
            return this;
        }

        @Override
        public SecureBuilder defaultOptions(Consumer<RocketRestOptions> optionsConfigurer) {
            super.defaultOptions(optionsConfigurer);
            return this;
        }

        @Override
        public SecureBuilder defaultOption(String key, Object value) {
            super.defaultOption(key, value);
            return this;
        }

        /**
         * Builds a new RocketRestSecureConfig instance.
         *
         * @return a new RocketRestSecureConfig instance
         */
        @Override
        public RocketRestSecureConfig build() {
            return new RocketRestSecureConfig(this);
        }
    }
    // Certificate options
    private final boolean customCertificateEnabled;
    private final String customCertificateFilename;
    private final String customCertificatePassword;

    // Package-private constructor for access by SecureBuilder
    protected RocketRestSecureConfig(SecureBuilder builder) {
        // Use the protected constructor from a parent
        super(builder);
        this.customCertificateEnabled = builder.customCertificateEnabled;
        this.customCertificateFilename = builder.customCertificateFilename;
        this.customCertificatePassword = builder.customCertificatePassword;
    }

    /**
     * Checks if a custom SSL certificate is enabled.
     *
     * @return true if a custom certificate is enabled
     */
    public boolean isCustomCertificateEnabled() {
        return customCertificateEnabled;
    }

    /**
     * Gets the custom certificate filename.
     *
     * @return the custom certificate filename
     */
    public String getCustomCertificateFilename() {
        return customCertificateFilename;
    }

    /**
     * Gets the custom certificate password.
     *
     * @return the custom certificate password
     */
    public String getCustomCertificatePassword() {
        return customCertificatePassword;
    }

    /**
     * Creates a new secure builder for RocketRestConfig.
     *
     * @param serviceUrl the base URL for the API service
     * @return a new secure builder instance
     */
    public static SecureBuilder secureBuilder(String serviceUrl) {
        return new SecureBuilder(serviceUrl);
    }
}
