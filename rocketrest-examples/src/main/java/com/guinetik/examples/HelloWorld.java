package com.guinetik.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main example runner that presents a menu of examples to run
 */
public class HelloWorld {
    // List of available examples
    private static final List<Example> EXAMPLES = new ArrayList<>();
    
    static {
        // Register all examples here
        EXAMPLES.add(new JsonTodoExample());
        EXAMPLES.add(new FakeJsonApiExample());
        EXAMPLES.add(new FluentApiExample());
        EXAMPLES.add(new RocketRestFluentExample());
        EXAMPLES.add(new RocketRestCircuitBreakerExample());
        EXAMPLES.add(new NasaApodExample());
        EXAMPLES.add(new PokeApiExample());
        EXAMPLES.add(new WeatherExample());
        EXAMPLES.add(new SapIdpExample());
        EXAMPLES.add(new AdpApiExample());
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
        // Add more examples as they are created
    }
    
    public static void main(String[] args) {
        System.out.println("=== RocketRest Examples ===");
        System.out.println("Choose an example to run:");
        
        // Display all available examples
        for (int i = 0; i < EXAMPLES.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, EXAMPLES.get(i).getName());
        }
        
        System.out.print("\nEnter number (1-" + EXAMPLES.size() + "): ");
        
        // Read user input
        Scanner scanner = new Scanner(System.in);
        int choice = 0;
        
        try {
            choice = Integer.parseInt(scanner.nextLine());
            
            if (choice < 1 || choice > EXAMPLES.size()) {
                System.out.println("Invalid choice. Exiting.");
                return;
            }
            
            // Run the selected example
            Example selectedExample = EXAMPLES.get(choice - 1);
            System.out.println("\nRunning: " + selectedExample.getName());
            System.out.println("===================================\n");
            
            selectedExample.run();
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
        } finally {
            scanner.close();
        }
    }
} 