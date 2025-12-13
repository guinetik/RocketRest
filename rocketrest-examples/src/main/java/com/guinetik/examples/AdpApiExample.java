package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.RocketRestSecureConfig;
import com.guinetik.rr.auth.AuthStrategy;
import com.guinetik.rr.auth.AuthStrategyFactory;
import com.guinetik.rr.auth.RocketSSL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.util.Scanner;

/**
 * Example demonstrating how to call the ADP API using:
 * 1. OAuth2 Client-Credentials flow (client_id / client_secret)
 * 2. Mutual TLS with a PKCS#12 certificate handled via {@link RocketSSL}
 */
public class AdpApiExample implements Example {

    // ====== User-supplied parameters ======
    private String clientId;
    private String clientSecret;
    private String certificatePath;
    private String certificatePassword;

    // ====== Constant ADP endpoints ======
    private String tokenUrl;
    private String baseUrl;
    private String endpoint; // resource path

    static {
        // Basic logging – keep the output similar to other examples
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
    }

    public static void main(String[] args) {
        AdpApiExample example = new AdpApiExample();
        // Read credentials from environment variables
        example.clientId = System.getenv("ADP_CLIENT_ID");
        example.clientSecret = System.getenv("ADP_CLIENT_SECRET");
        example.certificatePath = System.getenv("ADP_CERTIFICATE_FILE");
        example.certificatePassword = System.getenv("ADP_CERTIFICATE_PASSWORD");
        example.tokenUrl = "https://api.adp.com/auth/oauth/v2/token";
        example.baseUrl = "https://api.adp.com";
        example.endpoint = "/codelists/hr/v3/position-seeker-management/applicant-onboard-templates/WFN/1";
        example.run();
    }

    @Override
    public String getName() {
        return "ADP API (OAuth2 + SSL) Example";
    }

    @Override
    public void run() {
        System.out.println("\n=== ADP API Integration Demo ===");
        System.out.println("This example demonstrates connecting to ADP's API using RocketRest with:");
        System.out.println("1. Custom SSL Certificate (mutual TLS) - Required for most enterprise APIs");
        System.out.println("2. OAuth2 Client Credentials flow - Secure authentication using client ID/secret");
        System.out.println();
        System.out.println("The demo will:");
        System.out.println("- Load your PKCS12 (.p12) certificate and configure SSL");
        System.out.println("- Request an OAuth2 access token from ADP");
        System.out.println("- Make a sample API call to ADP's API");
        System.out.println("- Display the raw response from the server");
        System.out.println();
        try (Scanner scanner = new Scanner(System.in)) {
            // Prompt only for missing values
            if (clientId == null || clientId.trim().isEmpty()) {
                System.out.print("Client ID: ");
                clientId = scanner.nextLine();
            }
            if (clientSecret == null || clientSecret.trim().isEmpty()) {
                System.out.print("Client Secret: ");
                clientSecret = scanner.nextLine();
            }
            if (certificatePath == null || certificatePath.trim().isEmpty()) {
                System.out.print("Certificate (.p12) Path: ");
                certificatePath = scanner.nextLine();
            }
            if (certificatePassword == null || certificatePassword.trim().isEmpty()) {
                System.out.print("Certificate Password: ");
                certificatePassword = scanner.nextLine();
            }
            if (endpoint == null || endpoint.trim().isEmpty()) {
                System.out.print("API Endpoint (press Enter for default): ");
                String input = scanner.nextLine();
                if (!input.trim().isEmpty()) {
                    endpoint = input.trim();
                }
            }
        }

        try {
            // ------------------ SSL setup ------------------
            RocketSSL.SSLCertificate certInfo = new RocketSSL.SSLCertificate(certificatePath, certificatePassword);
            RocketSSL sslUtil = new RocketSSL();
            SSLContext sslContext = sslUtil.getSSLContext(certInfo);
            if (sslContext == null) {
                System.err.println("Failed to create SSL context – aborting example.");
                return;
            }
            // Make our SSL context the default so every HttpsURLConnection uses it
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            // ------------------ Auth strategy ------------------
            AuthStrategy authStrategy = AuthStrategyFactory.createOAuth2ClientCredentials(
                    clientId,
                    clientSecret,
                    tokenUrl
            );
            // Ensure token retrieval uses the same SSL context
            if (authStrategy instanceof RocketSSL.SSLAware) {
                ((RocketSSL.SSLAware) authStrategy).configureSsl(sslContext);
            }
            System.out.println("Refreshing ADP access token...");
            authStrategy.refreshCredentials();
            // ------------------ RocketRest configuration ------------------
            RocketRestSecureConfig config = RocketRestSecureConfig.secureBuilder(baseUrl)
                    .withCustomCertificate(certificatePath, certificatePassword)
                    .authStrategy(authStrategy)
                    .tokenUrl(tokenUrl)
                    .defaultOptions(opts -> {
                        opts.set(RocketRestOptions.LOG_REQUEST_BODY, true);
                        opts.set(RocketRestOptions.LOG_RESPONSE_BODY, true);
                    })
                    .build();
            // ------------------ Make the request ------------------
            RocketRest client = new RocketRest(config);
            System.out.println("Calling ADP API: " + endpoint);
            String response = client.get(endpoint, String.class);
            System.out.println("\nAPI Response:\n" + response);
            client.shutdown();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 