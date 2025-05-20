package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Example demonstrating how to use the PokéAPI with RocketRest's fluent API,
 * including pagination support.
 * 
 * API documentation: https://pokeapi.co/docs/v2
 */
public class PokeApiExample implements Example {
    
    private static final String POKEAPI_BASE_URL = "https://pokeapi.co/api/v2";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final Scanner scanner = new Scanner(System.in);
    
    // RocketRest client
    private RocketRest client;
    
    @Override
    public String getName() {
        return "PokéAPI Browser (with pagination)";
    }
    
    @Override
    public void run() {
        System.out.println("===============================");
        System.out.println("PokéAPI Browser with Pagination");
        System.out.println("===============================");
        System.out.println("This example demonstrates using RocketRest to access the PokéAPI");
        System.out.println("with pagination support and detailed Pokemon data.");
        
        try {
            // Initialize RocketRest client
            initializeClient();
            
            // Main menu for the example
            mainMenu();
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }
    
    /**
     * Initialize the RocketRest client with appropriate configuration
     */
    private void initializeClient() {
        // Create configuration
        RocketRestConfig config = RocketRestConfig.builder(POKEAPI_BASE_URL)
                .defaultOptions(options -> {
                    options.set(RocketRestOptions.LOGGING_ENABLED, true);
                    options.set(RocketRestOptions.TIMING_ENABLED, true);
                    // Enable response body logging for debugging
                    options.set(RocketRestOptions.LOG_RESPONSE_BODY, true);
                })
                .build();
        
        // Create client
        client = new RocketRest(POKEAPI_BASE_URL, config);
        System.out.println("RocketRest client initialized for PokéAPI\n");
    }
    
    /**
     * Display and handle the main menu
     */
    private void mainMenu() {
        boolean exit = false;
        
        while (!exit) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Browse Pokémon List");
            System.out.println("2. Search Pokémon by Name/ID");
            System.out.println("3. Browse Pokémon Types");
            System.out.println("4. Browse Abilities");
            System.out.println("5. Exit");
            System.out.print("\nEnter your choice (1-5): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    browsePokemonList();
                    break;
                case "2":
                    searchPokemon();
                    break;
                case "3":
                    browseTypes();
                    break;
                case "4":
                    browseAbilities();
                    break;
                case "5":
                    exit = true;
                    System.out.println("Exiting PokéAPI Browser. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    /**
     * Browse the Pokémon list with pagination
     */
    private void browsePokemonList() {
        System.out.println("\n=== Browsing Pokémon List ===");
        System.out.print("Enter page size (default is " + DEFAULT_PAGE_SIZE + "): ");
        String sizeInput = scanner.nextLine().trim();
        
        int pageSize = DEFAULT_PAGE_SIZE;
        if (!sizeInput.isEmpty()) {
            try {
                pageSize = Integer.parseInt(sizeInput);
                if (pageSize <= 0) {
                    System.out.println("Invalid page size. Using default: " + DEFAULT_PAGE_SIZE);
                    pageSize = DEFAULT_PAGE_SIZE;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Using default page size: " + DEFAULT_PAGE_SIZE);
            }
        }
        
        // Initial query parameters
        Map<String, String> params = new HashMap<>();
        params.put("limit", String.valueOf(pageSize));
        params.put("offset", "0");
        
        String currentUrl = "/pokemon";
        boolean browsing = true;
        
        while (browsing) {
            // Use the fluent API to fetch the page
            Result<PokemonListResponse, ApiError> result = client.fluent()
                    .get(currentUrl, PokemonListResponse.class, params);
            
            if (result.isSuccess()) {
                PokemonListResponse response = result.getValue();
                displayPokemonList(response);
                
                // Handle pagination
                browsing = handlePagination(response, params);
            } else {
                ApiError error = result.getError();
                System.out.println("❌ Error fetching Pokémon list: " + error.getMessage());
                browsing = false;
            }
        }
    }
    
    /**
     * Search for a specific Pokémon by name or ID
     */
    private void searchPokemon() {
        System.out.println("\n=== Search Pokémon ===");
        System.out.print("Enter Pokémon name or ID (e.g., 'pikachu' or '25'): ");
        String query = scanner.nextLine().trim().toLowerCase();
        
        if (query.isEmpty()) {
            System.out.println("Search canceled.");
            return;
        }
        
        System.out.println("Searching for: " + query);
        
        // Use the fluent API to fetch the Pokémon details
        Result<PokemonDetailResponse, ApiError> result = client.fluent()
                .get("/pokemon/" + query, PokemonDetailResponse.class);
        
        result.ifSuccess(this::displayPokemonDetail)
              .ifFailure(error -> {
                  System.out.println("❌ Error: " + error.getMessage());
                  if (error.getStatusCode() == 404) {
                      System.out.println("Pokémon not found. Check the name or ID and try again.");
                  }
              });
    }
    
    /**
     * Browse Pokémon types
     */
    private void browseTypes() {
        System.out.println("\n=== Browsing Pokémon Types ===");
        
        // Use the fluent API to fetch the types list
        Result<TypeListResponse, ApiError> result = client.fluent()
                .get("/type", TypeListResponse.class);
        
        result.ifSuccess(this::displayTypesList)
              .ifFailure(error -> 
                  System.out.println("❌ Error fetching types: " + error.getMessage())
              );
    }
    
    /**
     * Browse Pokémon abilities
     */
    private void browseAbilities() {
        System.out.println("\n=== Browsing Pokémon Abilities ===");
        
        Map<String, String> params = new HashMap<>();
        params.put("limit", "20");
        
        // Use the fluent API to fetch the abilities list
        Result<AbilityListResponse, ApiError> result = client.fluent()
                .get("/ability", AbilityListResponse.class, params);
        
        result.ifSuccess(this::displayAbilitiesList)
              .ifFailure(error -> 
                  System.out.println("❌ Error fetching abilities: " + error.getMessage())
              );
    }
    
    /**
     * Display the list of Pokémon
     */
    private void displayPokemonList(PokemonListResponse response) {
        System.out.println("\n--- Pokémon List ---");
        System.out.println("Total Pokémon: " + response.getCount());
        
        List<NamedResource> results = response.getResults();
        if (results.isEmpty()) {
            System.out.println("No Pokémon found on this page.");
            return;
        }
        
        System.out.println("\nPokémon on this page:");
        for (int i = 0; i < results.size(); i++) {
            NamedResource pokemon = results.get(i);
            String url = pokemon.getUrl();
            String id = "?";
            
            // Extract ID from URL
            if (url != null && url.endsWith("/")) {
                String[] parts = url.split("/");
                if (parts.length > 0) {
                    id = parts[parts.length - 1];
                    if (id.isEmpty()) {
                        id = parts[parts.length - 2];
                    }
                }
            }
            
            System.out.printf("%2d. %-15s (ID: %s)%n", (i + 1), pokemon.getName(), id);
        }
    }
    
    /**
     * Handle pagination navigation
     * 
     * @return true if continuing to browse, false if exiting
     */
    private boolean handlePagination(PokemonListResponse response, Map<String, String> params) {
        String next = response.getNext();
        String previous = response.getPrevious();
        
        System.out.println("\n--- Navigation ---");
        System.out.println("N - Next page" + (next == null ? " (not available)" : ""));
        System.out.println("P - Previous page" + (previous == null ? " (not available)" : ""));
        System.out.println("D - View details of a Pokémon");
        System.out.println("M - Return to main menu");
        System.out.print("\nEnter choice: ");
        
        String choice = scanner.nextLine().trim().toUpperCase();
        
        switch (choice) {
            case "N":
                if (next != null) {
                    // Extract the query parameters from the next URL
                    extractParamsFromUrl(next, params);
                    return true;
                } else {
                    System.out.println("No next page available.");
                    return true;
                }
            case "P":
                if (previous != null) {
                    // Extract the query parameters from the previous URL
                    extractParamsFromUrl(previous, params);
                    return true;
                } else {
                    System.out.println("No previous page available.");
                    return true;
                }
            case "D":
                viewPokemonDetail(response.getResults());
                return true;
            case "M":
                return false;
            default:
                System.out.println("Invalid choice.");
                return true;
        }
    }
    
    /**
     * Extract pagination parameters from a URL
     */
    private void extractParamsFromUrl(String url, Map<String, String> params) {
        // Extract the query part of the URL
        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            String query = url.substring(queryIndex + 1);
            String[] pairs = query.split("&");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
    }
    
    /**
     * Allow the user to view details of a specific Pokémon from the list
     */
    private void viewPokemonDetail(List<NamedResource> pokemonList) {
        System.out.print("\nEnter the number of the Pokémon to view (1-" + pokemonList.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < pokemonList.size()) {
                NamedResource pokemon = pokemonList.get(index);
                System.out.println("\nFetching details for " + pokemon.getName() + "...");
                
                // Extract ID from URL
                String url = pokemon.getUrl();
                String id = "";
                if (url != null && url.endsWith("/")) {
                    String[] parts = url.split("/");
                    if (parts.length > 0) {
                        id = parts[parts.length - 1];
                        if (id.isEmpty()) {
                            id = parts[parts.length - 2];
                        }
                    }
                }
                
                // Fetch and display Pokémon details
                Result<PokemonDetailResponse, ApiError> result = client.fluent()
                        .get("/pokemon/" + id, PokemonDetailResponse.class);
                
                result.ifSuccess(this::displayPokemonDetail)
                      .ifFailure(error -> System.out.println("❌ Error: " + error.getMessage()));
            } else {
                System.out.println("Invalid selection. Please try again.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
        }
    }
    
    /**
     * Display detailed information about a Pokémon
     */
    private void displayPokemonDetail(PokemonDetailResponse pokemon) {
        System.out.println("\n=== Pokémon Details ===");
        System.out.println("Name: " + capitalize(pokemon.getName()));
        System.out.println("ID: " + pokemon.getId());
        System.out.println("Height: " + (pokemon.getHeight() / 10.0) + " m");
        System.out.println("Weight: " + (pokemon.getWeight() / 10.0) + " kg");
        
        // Display types
        System.out.println("\nTypes:");
        List<TypeInfo> types = pokemon.getTypes();
        if (types != null) {
            for (TypeInfo typeInfo : types) {
                if (typeInfo.getType() != null) {
                    System.out.println("- " + capitalize(typeInfo.getType().getName()));
                }
            }
        }
        
        // Display abilities
        System.out.println("\nAbilities:");
        List<AbilityInfo> abilities = pokemon.getAbilities();
        if (abilities != null) {
            for (AbilityInfo abilityInfo : abilities) {
                if (abilityInfo.getAbility() != null) {
                    String hiddenTag = abilityInfo.isHidden() ? " (Hidden)" : "";
                    System.out.println("- " + capitalize(abilityInfo.getAbility().getName()) + hiddenTag);
                }
            }
        }
        
        // Display base stats
        System.out.println("\nBase Stats:");
        List<StatInfo> stats = pokemon.getStats();
        if (stats != null) {
            for (StatInfo statInfo : stats) {
                if (statInfo.getStat() != null) {
                    String statName = statInfo.getStat().getName().replace("-", " ");
                    System.out.printf("- %-20s: %d%n", capitalize(statName), statInfo.getBase_stat());
                }
            }
        } else {
            System.out.println("No stats available");
        }
        
        // Display moves (just a few)
        List<MoveInfo> moves = pokemon.getMoves();
        if (moves != null && !moves.isEmpty()) {
            System.out.println("\nSample Moves (first 5):");
            for (int i = 0; i < Math.min(5, moves.size()); i++) {
                MoveInfo moveInfo = moves.get(i);
                if (moveInfo.getMove() != null) {
                    System.out.println("- " + capitalize(moveInfo.getMove().getName().replace("-", " ")));
                }
            }
            System.out.println("... and " + (moves.size() - Math.min(5, moves.size())) + " more moves");
        }
        
        // Wait for user to press Enter before continuing
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Display the list of Pokémon types
     */
    private void displayTypesList(TypeListResponse response) {
        System.out.println("\n--- Pokémon Types ---");
        List<NamedResource> results = response.getResults();
        
        if (results == null || results.isEmpty()) {
            System.out.println("No types found.");
            return;
        }
        
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("%2d. %s%n", (i + 1), capitalize(results.get(i).getName()));
        }
        
        // Option to view type details
        System.out.print("\nEnter a type number for details or press Enter to continue: ");
        String input = scanner.nextLine().trim();
        
        if (!input.isEmpty()) {
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < results.size()) {
                    NamedResource type = results.get(index);
                    System.out.println("\nFetching details for " + type.getName() + " type...");
                    
                    // Fetch and display type details
                    fetchTypeDetails(type.getName());
                } else {
                    System.out.println("Invalid selection.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        }
    }
    
    /**
     * Display the list of Pokémon abilities
     */
    private void displayAbilitiesList(AbilityListResponse response) {
        System.out.println("\n--- Pokémon Abilities ---");
        List<NamedResource> results = response.getResults();
        
        if (results == null || results.isEmpty()) {
            System.out.println("No abilities found.");
            return;
        }
        
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("%2d. %s%n", (i + 1), capitalize(results.get(i).getName().replace("-", " ")));
        }
        
        // Option to view ability details
        System.out.print("\nEnter an ability number for details or press Enter to continue: ");
        String input = scanner.nextLine().trim();
        
        if (!input.isEmpty()) {
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < results.size()) {
                    NamedResource ability = results.get(index);
                    System.out.println("\nFetching details for " + ability.getName() + " ability...");
                    
                    // Future enhancement: Fetch and display ability details
                    System.out.println("Ability details feature coming soon!");
                } else {
                    System.out.println("Invalid selection.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        }
    }
    
    /**
     * Helper method to capitalize a string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Fetch and display details for a specific Pokémon type
     */
    private void fetchTypeDetails(String typeName) {
        Result<TypeDetailResponse, ApiError> result = client.fluent()
                .get("/type/" + typeName, TypeDetailResponse.class);
        
        result.ifSuccess(this::displayTypeDetails)
              .ifFailure(error -> System.out.println("❌ Error: " + error.getMessage()));
    }
    
    /**
     * Display detailed information about a Pokémon type
     */
    private void displayTypeDetails(TypeDetailResponse type) {
        System.out.println("\n=== Type Details: " + capitalize(type.getName()) + " ===");
        System.out.println("ID: " + type.getId());
        
        DamageRelations damageRelations = type.getDamageRelations();
        if (damageRelations != null) {
            System.out.println("\n--- Damage Relations ---");
            
            System.out.println("\nWeak against (2x damage from):");
            displayNamedResources(damageRelations.getDoubleDamageFrom());
            
            System.out.println("\nStrong against (2x damage to):");
            displayNamedResources(damageRelations.getDoubleDamageTo());
            
            System.out.println("\nResistant to (½ damage from):");
            displayNamedResources(damageRelations.getHalfDamageFrom());
            
            System.out.println("\nNot very effective against (½ damage to):");
            displayNamedResources(damageRelations.getHalfDamageTo());
            
            System.out.println("\nImmune to (no damage from):");
            displayNamedResources(damageRelations.getNoDamageFrom());
            
            System.out.println("\nDoesn't affect (no damage to):");
            displayNamedResources(damageRelations.getNoDamageTo());
        }
        
        // Display some moves of this type
        List<NamedResource> moves = type.getMoves();
        if (moves != null && !moves.isEmpty()) {
            System.out.println("\n--- Sample Moves ---");
            int displayCount = Math.min(10, moves.size());
            for (int i = 0; i < displayCount; i++) {
                System.out.println("- " + capitalize(moves.get(i).getName().replace("-", " ")));
            }
            
            if (moves.size() > displayCount) {
                System.out.println("... and " + (moves.size() - displayCount) + " more moves");
            }
        }
        
        // Wait for user to press Enter before continuing
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Helper method to display a list of named resources
     */
    private void displayNamedResources(List<NamedResource> resources) {
        if (resources == null || resources.isEmpty()) {
            System.out.println("None");
            return;
        }
        
        for (NamedResource resource : resources) {
            System.out.println("- " + capitalize(resource.getName()));
        }
    }
    
    //
    // Model classes for PokéAPI responses
    //
    
    /**
     * Base response for paginated results
     */
    public static class PaginatedResponse<T> {
        private int count;
        private String next;
        private String previous;
        private List<T> results;
        
        public int getCount() {
            return count;
        }
        
        public void setCount(int count) {
            this.count = count;
        }
        
        public String getNext() {
            return next;
        }
        
        public void setNext(String next) {
            this.next = next;
        }
        
        public String getPrevious() {
            return previous;
        }
        
        public void setPrevious(String previous) {
            this.previous = previous;
        }
        
        public List<T> getResults() {
            return results;
        }
        
        public void setResults(List<T> results) {
            this.results = results;
        }
    }
    
    /**
     * Response for the Pokémon list endpoint
     */
    public static class PokemonListResponse extends PaginatedResponse<NamedResource> {
    }
    
    /**
     * Response for the Types list endpoint
     */
    public static class TypeListResponse extends PaginatedResponse<NamedResource> {
    }
    
    /**
     * Response for the Abilities list endpoint
     */
    public static class AbilityListResponse extends PaginatedResponse<NamedResource> {
    }
    
    /**
     * Named resource (name and URL) used throughout the API
     */
    public static class NamedResource {
        private String name;
        private String url;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    /**
     * Detailed Pokémon response
     */
    public static class PokemonDetailResponse {
        private int id;
        private String name;
        private int height;
        private int weight;
        private List<TypeInfo> types;
        private List<AbilityInfo> abilities;
        private List<StatInfo> stats;
        private List<MoveInfo> moves;
        
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getHeight() {
            return height;
        }
        
        public void setHeight(int height) {
            this.height = height;
        }
        
        public int getWeight() {
            return weight;
        }
        
        public void setWeight(int weight) {
            this.weight = weight;
        }
        
        public List<TypeInfo> getTypes() {
            return types;
        }
        
        public void setTypes(List<TypeInfo> types) {
            this.types = types;
        }
        
        public List<AbilityInfo> getAbilities() {
            return abilities;
        }
        
        public void setAbilities(List<AbilityInfo> abilities) {
            this.abilities = abilities;
        }
        
        public List<StatInfo> getStats() {
            return stats;
        }
        
        public void setStats(List<StatInfo> stats) {
            this.stats = stats;
        }
        
        public List<MoveInfo> getMoves() {
            return moves;
        }
        
        public void setMoves(List<MoveInfo> moves) {
            this.moves = moves;
        }
    }
    
    /**
     * Type information for a Pokémon
     */
    public static class TypeInfo {
        private int slot;
        private NamedResource type;
        
        public int getSlot() {
            return slot;
        }
        
        public void setSlot(int slot) {
            this.slot = slot;
        }
        
        public NamedResource getType() {
            return type;
        }
        
        public void setType(NamedResource type) {
            this.type = type;
        }
    }
    
    /**
     * Ability information for a Pokémon
     */
    public static class AbilityInfo {
        private boolean is_hidden;
        private int slot;
        private NamedResource ability;
        
        public boolean isHidden() {
            return is_hidden;
        }
        
        public void setHidden(boolean hidden) {
            is_hidden = hidden;
        }
        
        public int getSlot() {
            return slot;
        }
        
        public void setSlot(int slot) {
            this.slot = slot;
        }
        
        public NamedResource getAbility() {
            return ability;
        }
        
        public void setAbility(NamedResource ability) {
            this.ability = ability;
        }
    }
    
    /**
     * Stat information for a Pokémon
     */
    public static class StatInfo {
        // Using exact field names from the API for proper JSON mapping
        private int base_stat; // The base value of the stat
        private int effort;
        private NamedResource stat;
        
        /**
         * Gets the base stat value.
         * The field is named base_stat in JSON but we use camelCase for Java methods.
         * 
         * @return The base stat value
         */
        public int getBaseStat() {
            return base_stat;
        }
        
        /**
         * Gets the base stat value directly.
         * This method is added for direct access to the field.
         * 
         * @return The base stat value
         */
        public int getBase_stat() {
            return base_stat;
        }
        
        /**
         * Sets the base stat value.
         * 
         * @param baseStat The base stat value
         */
        public void setBaseStat(int baseStat) {
            this.base_stat = baseStat;
        }
        
        /**
         * Sets the base stat value directly.
         * 
         * @param base_stat The base stat value
         */
        public void setBase_stat(int base_stat) {
            this.base_stat = base_stat;
        }
        
        public int getEffort() {
            return effort;
        }
        
        public void setEffort(int effort) {
            this.effort = effort;
        }
        
        public NamedResource getStat() {
            return stat;
        }
        
        public void setStat(NamedResource stat) {
            this.stat = stat;
        }
        
        @Override
        public String toString() {
            return "StatInfo{" +
                    "base_stat=" + base_stat +
                    ", effort=" + effort +
                    ", stat=" + (stat != null ? stat.getName() : "null") +
                    '}';
        }
    }
    
    /**
     * Move information for a Pokémon
     */
    public static class MoveInfo {
        private NamedResource move;
        
        public NamedResource getMove() {
            return move;
        }
        
        public void setMove(NamedResource move) {
            this.move = move;
        }
    }
    
    /**
     * Response for the Type details endpoint
     */
    public static class TypeDetailResponse {
        private int id;
        private String name;
        private DamageRelations damage_relations;
        private List<NamedResource> moves;
        
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public DamageRelations getDamageRelations() {
            return damage_relations;
        }
        
        public void setDamageRelations(DamageRelations damageRelations) {
            this.damage_relations = damageRelations;
        }
        
        public List<NamedResource> getMoves() {
            return moves;
        }
        
        public void setMoves(List<NamedResource> moves) {
            this.moves = moves;
        }
    }
    
    /**
     * Damage relations information for a type
     */
    public static class DamageRelations {
        private List<NamedResource> double_damage_from;
        private List<NamedResource> double_damage_to;
        private List<NamedResource> half_damage_from;
        private List<NamedResource> half_damage_to;
        private List<NamedResource> no_damage_from;
        private List<NamedResource> no_damage_to;
        
        public List<NamedResource> getDoubleDamageFrom() {
            return double_damage_from;
        }
        
        public void setDoubleDamageFrom(List<NamedResource> doubleDamageFrom) {
            this.double_damage_from = doubleDamageFrom;
        }
        
        public List<NamedResource> getDoubleDamageTo() {
            return double_damage_to;
        }
        
        public void setDoubleDamageTo(List<NamedResource> doubleDamageTo) {
            this.double_damage_to = doubleDamageTo;
        }
        
        public List<NamedResource> getHalfDamageFrom() {
            return half_damage_from;
        }
        
        public void setHalfDamageFrom(List<NamedResource> halfDamageFrom) {
            this.half_damage_from = halfDamageFrom;
        }
        
        public List<NamedResource> getHalfDamageTo() {
            return half_damage_to;
        }
        
        public void setHalfDamageTo(List<NamedResource> halfDamageTo) {
            this.half_damage_to = halfDamageTo;
        }
        
        public List<NamedResource> getNoDamageFrom() {
            return no_damage_from;
        }
        
        public void setNoDamageFrom(List<NamedResource> noDamageFrom) {
            this.no_damage_from = noDamageFrom;
        }
        
        public List<NamedResource> getNoDamageTo() {
            return no_damage_to;
        }
        
        public void setNoDamageTo(List<NamedResource> noDamageTo) {
            this.no_damage_to = noDamageTo;
        }
    }
} 