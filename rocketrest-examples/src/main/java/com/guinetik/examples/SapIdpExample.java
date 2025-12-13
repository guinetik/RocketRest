package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.auth.AuthStrategy;
import com.guinetik.rr.auth.AuthStrategyFactory;

import java.util.Scanner;

/**
 * Example demonstrating SAP IDP authentication and API usage.
 * This example:
 * 1. Prompts for SAP IDP credentials
 * 2. Authenticates using IDP
 * 3. Prints access token
 * 4. Makes a test API call
 */
public class SapIdpExample implements Example {

    private String clientId;
    private String userId;
    private String privateKey;
    private String companyId;
    private String grantType;
    private String idpUrl;
    private String tokenUrl;
    private String baseUrl;
    private String endpoint;

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
    }

    public static void main(String[] args) {
        SapIdpExample example = new SapIdpExample();

        // Read values from environment variables
        example.clientId = System.getenv("SAP_CLIENT_ID");
        example.userId = System.getenv("SAP_USER_ID");
        example.privateKey = System.getenv("SAP_PRIVATE_KEY");
        example.companyId = System.getenv("SAP_COMPANY_ID");
        example.grantType = System.getenv("SAP_GRANT_TYPE");
        example.idpUrl = System.getenv("SAP_IDP_URL");
        example.tokenUrl = System.getenv("SAP_TOKEN_URL");
        example.baseUrl = System.getenv("SAP_BASE_URL");

        example.run();
    }
    
    @Override
    public String getName() {
        return "SAP IDP Authentication Example";
    }

    @Override
    public void run() {
        System.out.println("\n=== SAP IDP Authentication Example ===");
        System.out.println("This example demonstrates connecting to SAP's API using RocketRest with:");
        System.out.println("1. OAuth2 SAML2 Bearer flow via SAP IDP");
        System.out.println("2. Secure authentication to access SAP's SuccessFactors APIs");
        System.out.println();
        System.out.println("You can set these environment variables before running to skip manual input:");
        System.out.println("- SAP_CLIENT_ID");
        System.out.println("- SAP_USER_ID");
        System.out.println("- SAP_PRIVATE_KEY");
        System.out.println("- SAP_COMPANY_ID");
        System.out.println("- SAP_GRANT_TYPE");
        System.out.println("- SAP_IDP_URL");
        System.out.println("- SAP_TOKEN_URL");
        System.out.println("- SAP_BASE_URL");
        System.out.println("\n-----------------------------------------\n");

        try (Scanner scanner = new Scanner(System.in)) {
            // Get SAP IDP credentials
            if( this.clientId == null || this.clientId.trim().isEmpty() ) {
                System.out.println("\nEnter SAP IDP credentials:");
                System.out.print("Client ID: ");
                this.clientId = scanner.nextLine();
            }
            
            if( this.userId == null || this.userId.trim().isEmpty() ) {
                System.out.print("User ID: ");
                this.userId = scanner.nextLine();
            }
            
            if( this.privateKey == null || this.privateKey.trim().isEmpty() ) {
                System.out.print("Private Key: ");
                this.privateKey = scanner.nextLine();
            }
            
            if( this.companyId == null || this.companyId.trim().isEmpty() ) {
                System.out.print("Company ID (optional, press Enter to skip): ");
                this.companyId = scanner.nextLine();
                if (this.companyId.trim().isEmpty()) {
                    this.companyId = null;
                }
            }
            
            if( this.grantType == null || this.grantType.trim().isEmpty() ) {
                System.out.print("Grant Type: ");
                this.grantType = scanner.nextLine();
            }
            
            if( this.idpUrl == null || this.idpUrl.trim().isEmpty() ) {
                System.out.print("IDP URL: ");
                this.idpUrl = scanner.nextLine();
            }
            
            if( this.tokenUrl == null || this.tokenUrl.trim().isEmpty() ) {
                System.out.print("Token URL: ");
                this.tokenUrl = scanner.nextLine();
            }
            
            if( this.baseUrl == null || this.baseUrl.trim().isEmpty() ) {
                System.out.print("SAP API Base URL: ");
                this.baseUrl = scanner.nextLine();
            }
            
            System.out.print("API Endpoint (press Enter to use default '/User?$top=1&$format=json&$select=userId'): ");
            this.endpoint = scanner.nextLine();
            if (this.endpoint.trim().isEmpty()) {
                this.endpoint = "/User?$top=1&$format=json&$select=userId";
            }

            // Create IDP auth strategy
            AuthStrategy authStrategy = AuthStrategyFactory.createOAuth2Assertion(
                this.clientId,
                    this.userId,
                    this.privateKey,
                    this.companyId,
                    this.grantType,
                    this.idpUrl,
                    this.tokenUrl
            );

            System.out.println("Refreshing SAP Token");
            authStrategy.refreshCredentials();

            // Create RocketRest client
            RocketRest client = new RocketRest(
                RocketRestConfig.builder(this.baseUrl)
                        .defaultOptions(options -> {
                            options.set(RocketRestOptions.RETRY_ENABLED, true);
                            options.set(RocketRestOptions.MAX_RETRIES, 3);
                            options.set(RocketRestOptions.LOG_REQUEST_BODY, true);
                            options.set(RocketRestOptions.LOG_RESPONSE_BODY, true);
                        })
                    .tokenUrl(this.tokenUrl)
                    .authStrategy(authStrategy)
                    .tokenUrl(this.tokenUrl)
                    .build()
            );

            // Print access token
            System.out.println("\nAccess Token:");
            System.out.println(client.getAccessToken());

            // Print Expiration
            System.out.println("\nExpiration:");
            System.out.println(client.getTokenExpiryTime());

            // Make API call
            System.out.println("\nMaking API call to: " + this.endpoint);
            try {
                String response = client.get(this.endpoint, String.class);
                // Pretty print the response
                System.out.println("\nAPI Response:");
                System.out.println(response);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Always shutdown the client to release resources
                client.shutdown();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 