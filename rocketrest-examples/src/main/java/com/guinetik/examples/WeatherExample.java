package com.guinetik.examples;

import com.guinetik.rr.RocketRest;
import com.guinetik.rr.RocketRestConfig;
import com.guinetik.rr.RocketRestOptions;
import com.guinetik.rr.result.ApiError;
import com.guinetik.rr.result.Result;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.function.Function;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Example demonstrating how to use RocketRest's async API to fetch weather data
 * from wttr.in service.
 */
public class WeatherExample implements Example {
    
    private static final String WTTR_API_URL = "https://wttr.in";
    private static final Scanner scanner = new Scanner(System.in);
    private RocketRest client;
    
    @Override
    public String getName() {
        return "Weather API (using async)";
    }
    
    @Override
    public void run() {
        System.out.println("==============================");
        System.out.println("Weather API Example (async)");
        System.out.println("==============================");
        System.out.println("This example demonstrates using RocketRest's async API");
        System.out.println("to fetch weather data from wttr.in service.\n");
        
        try {
            // Initialize RocketRest client
            initializeClient();
            
            // Main menu
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
        RocketRestConfig config = RocketRestConfig.builder(WTTR_API_URL)
                .defaultOptions(options -> {
                    options.set(RocketRestOptions.LOGGING_ENABLED, true);
                    options.set(RocketRestOptions.TIMING_ENABLED, true);
                    options.set(RocketRestOptions.LOG_REQUEST_BODY, true);
                    options.set(RocketRestOptions.LOG_RESPONSE_BODY, true);
                })
                .build();
        
        client = new RocketRest(WTTR_API_URL, config);
        System.out.println("RocketRest client initialized for wttr.in API\n");
    }
    
    /**
     * Main menu for the Weather example
     */
    private void mainMenu() {
        boolean exit = false;
        
        while (!exit) {
            System.out.println("\n--- Weather API Menu ---");
            System.out.println("1. Get Current Weather by Location");
            System.out.println("2. Get Weather Forecast by Location");
            System.out.println("3. Compare Weather of Multiple Locations");
            System.out.println("4. Exit");
            System.out.print("\nEnter your choice (1-4): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    getCurrentWeather();
                    break;
                case "2":
                    getWeatherForecast();
                    break;
                case "3":
                    compareMultipleLocations();
                    break;
                case "4":
                    exit = true;
                    System.out.println("Exiting Weather API Example. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    /**
     * URL encode a location name to handle spaces and special characters
     */
    private String encodeLocation(String location) {
        try {
            return URLEncoder.encode(location, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20"); // Some APIs prefer %20 over + for spaces
        } catch (UnsupportedEncodingException e) {
            System.out.println("Warning: Error encoding location. Using raw value.");
            return location;
        }
    }
    
    /**
     * Get current weather for a specified location using a functional approach
     */
    private void getCurrentWeather() {
        System.out.println("\n=== Current Weather ===");
        System.out.print("Enter location (city name or coordinates): ");
        String location = scanner.nextLine().trim();
        
        if (location.isEmpty()) {
            System.out.println("Location cannot be empty. Returning to menu.");
            return;
        }
        
        // Encode the location to handle spaces and special characters
        String encodedLocation = encodeLocation(location);
        
        System.out.println("\nFetching current weather for " + location + "...");
        
        // Construct query parameters
        Map<String, String> params = new HashMap<>();
        params.put("format", "j1");
        
        // Define a function to get additional information if needed
        Function<WeatherResponse, CompletableFuture<WeatherResponse>> enrichWeatherData = 
                response -> {
                    // In a real application, you might fetch additional data here
                    // and combine it with the original response
                    
                    // This is just a placeholder to demonstrate composition
                    System.out.println("Processing weather data...");
                    
                    // For demonstration, just return the original response
                    return CompletableFuture.completedFuture(response);
                };
        
        // Use the async API with functional composition
        client.async()
                .get("/" + encodedLocation, WeatherResponse.class, params)
                // Chain additional async operations if needed
                .thenCompose(enrichWeatherData)
                // Process and display the result
                .thenAccept(response -> {
                    System.out.println("Weather data received successfully!");
                    displayCurrentWeather(response);
                })
                // Handle errors
                .exceptionally(ex -> {
                    System.out.println("❌ Error fetching weather: " + ex.getMessage());
                    return null;
                })
                // Wait for completion (blocking here for the example's simplicity)
                .join();
    }
    
    /**
     * Get weather forecast for a specified location using a reactive approach
     */
    private void getWeatherForecast() {
        System.out.println("\n=== Weather Forecast ===");
        System.out.print("Enter location (city name or coordinates): ");
        String location = scanner.nextLine().trim();
        
        if (location.isEmpty()) {
            System.out.println("Location cannot be empty. Returning to menu.");
            return;
        }
        
        // Encode the location to handle spaces and special characters
        String encodedLocation = encodeLocation(location);
        
        System.out.println("\nFetching weather forecast for " + location + "...");
        
        // Construct query parameters
        Map<String, String> params = new HashMap<>();
        params.put("format", "j1");
        
        // Use the async API with functional composition
        client.async()
                .get("/" + encodedLocation, WeatherResponse.class, params)
                // Transform the result
                .thenApply(response -> {
                    // Add processing logic here if needed
                    return response;
                })
                // Handle successful completion
                .thenAccept(this::displayForecast)
                // Handle errors
                .exceptionally(ex -> {
                    System.out.println("❌ Error fetching forecast: " + ex.getMessage());
                    return null;
                })
                // Wait for completion (blocking here for the example's simplicity)
                .join();
    }
    
    /**
     * Compare weather of multiple locations asynchronously using a functional approach
     */
    private void compareMultipleLocations() {
        System.out.println("\n=== Compare Multiple Locations ===");
        System.out.println("Enter locations (separated by commas):");
        String input = scanner.nextLine().trim();
        
        if (input.isEmpty()) {
            System.out.println("No locations provided. Returning to menu.");
            return;
        }
        
        // Convert input to a list of locations
        List<String> locations = Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        System.out.println("\nFetching weather data for " + locations.size() + " locations...");
        
        // Query parameters
        Map<String, String> params = new HashMap<>();
        params.put("format", "j1");
        
        // Create a CompletableFuture for each location
        List<CompletableFuture<Pair<String, WeatherResponse>>> futures = locations.stream()
                .map(location -> {
                    // Encode the location to handle spaces and special characters
                    String encodedLocation = encodeLocation(location);
                    
                    // Create a future that will contain a pair of location and weather response
                    return client.async()
                            .get("/" + encodedLocation, WeatherResponse.class, params)
                            .thenApply(response -> new Pair<>(location, response))
                            .exceptionally(ex -> {
                                System.out.println("❌ Error for " + location + ": " + ex.getMessage());
                                return new Pair<>(location, null);
                            });
                })
                .collect(Collectors.toList());
        
        // Combine all futures into a single future that completes when all complete
        CompletableFuture<List<Pair<String, WeatherResponse>>> allFutures = 
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> futures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));
        
        // Process the results when all futures complete
        allFutures.thenAccept(results -> {
            System.out.println("\n=== Weather Comparison ===");
            
            results.forEach(pair -> {
                String location = pair.getFirst();
                WeatherResponse response = pair.getSecond();
                
                System.out.println("\nLocation: " + location);
                if (response != null) {
                    displayWeatherSummary(response);
                } else {
                    System.out.println("❌ No data available");
                }
            });
        }).join(); // Wait for all processing to complete
    }
    
    /**
     * Display current weather information
     */
    private void displayCurrentWeather(WeatherResponse response) {
        if (response == null || response.getCurrent_condition() == null || response.getCurrent_condition().isEmpty()) {
            System.out.println("No weather data available.");
            return;
        }
        
        CurrentCondition current = response.getCurrent_condition().get(0);
        NearestArea area = response.getNearest_area() != null && !response.getNearest_area().isEmpty() 
                ? response.getNearest_area().get(0) : null;
        
        // Debug output to see raw values
        System.out.println("\nDEBUG - Current condition raw data: " + current);
        
        System.out.println("\n=== Current Weather ===");
        
        // Display location information
        if (area != null) {
            String areaName = area.getAreaName() != null && !area.getAreaName().isEmpty() 
                    ? area.getAreaName().get(0).getValue() : "Unknown";
            String country = area.getCountry() != null && !area.getCountry().isEmpty() 
                    ? area.getCountry().get(0).getValue() : "";
            String region = area.getRegion() != null && !area.getRegion().isEmpty() 
                    ? area.getRegion().get(0).getValue() : "";
            
            System.out.println("Location: " + areaName + 
                    (region.isEmpty() ? "" : ", " + region) + 
                    (country.isEmpty() ? "" : ", " + country));
        }
        
        // Display observation time
        System.out.println("Observation Time: " + current.getLocalObsDateTime());
        
        // Display current weather conditions
        String weatherDesc = current.getWeatherDesc() != null && !current.getWeatherDesc().isEmpty() 
                ? current.getWeatherDesc().get(0).getValue() : "Unknown";
        System.out.println("Conditions: " + weatherDesc);
        
        // Display temperature
        System.out.println("Temperature: " + current.getTemp_C() + "°C / " + current.getTemp_F() + "°F");
        System.out.println("Feels Like: " + current.getFeelsLikeC() + "°C / " + current.getFeelsLikeF() + "°F");
        
        // Display other weather details
        System.out.println("Humidity: " + current.getHumidity() + "%");
        System.out.println("Wind: " + current.getWindspeedKmph() + " km/h (" + current.getWinddir16Point() + ")");
        System.out.println("Pressure: " + current.getPressure() + " hPa");
        System.out.println("Cloud Cover: " + current.getCloudcover() + "%");
        System.out.println("Visibility: " + current.getVisibility() + " km");
        System.out.println("UV Index: " + current.getUvIndex());
    }
    
    /**
     * Display weather forecast information
     */
    private void displayForecast(WeatherResponse response) {
        if (response == null || response.getWeather() == null || response.getWeather().isEmpty()) {
            System.out.println("No forecast data available.");
            return;
        }
        
        Weather today = response.getWeather().get(0);
        NearestArea area = response.getNearest_area() != null && !response.getNearest_area().isEmpty() 
                ? response.getNearest_area().get(0) : null;
        
        System.out.println("\n=== Weather Forecast ===");
        
        // Display location information
        if (area != null) {
            String areaName = area.getAreaName() != null && !area.getAreaName().isEmpty() 
                    ? area.getAreaName().get(0).getValue() : "Unknown";
            String country = area.getCountry() != null && !area.getCountry().isEmpty() 
                    ? area.getCountry().get(0).getValue() : "";
            
            System.out.println("Location: " + areaName + (country.isEmpty() ? "" : ", " + country));
        }
        
        // Display today's forecast
        System.out.println("\nForecast for " + today.getDate() + ":");
        System.out.println("Temperature Range: " + today.getMintempC() + "°C to " + today.getMaxtempC() + "°C");
        System.out.println("Average Temperature: " + today.getAvgtempC() + "°C");
        System.out.println("Sunshine Hours: " + today.getSunHour());
        
        // Display astronomy information
        if (today.getAstronomy() != null && !today.getAstronomy().isEmpty()) {
            Astronomy astronomy = today.getAstronomy().get(0);
            System.out.println("\nAstronomy:");
            System.out.println("Sunrise: " + astronomy.getSunrise());
            System.out.println("Sunset: " + astronomy.getSunset());
            System.out.println("Moonrise: " + astronomy.getMoonrise());
            System.out.println("Moonset: " + astronomy.getMoonset());
            System.out.println("Moon Phase: " + astronomy.getMoon_phase());
            System.out.println("Moon Illumination: " + astronomy.getMoon_illumination() + "%");
        }
    }
    
    /**
     * Display a summary of weather data for comparison
     */
    private void displayWeatherSummary(WeatherResponse response) {
        if (response == null) {
            System.out.println("No data available.");
            return;
        }
        
        if (response.getCurrent_condition() != null && !response.getCurrent_condition().isEmpty()) {
            CurrentCondition current = response.getCurrent_condition().get(0);
            String weatherDesc = current.getWeatherDesc() != null && !current.getWeatherDesc().isEmpty() 
                    ? current.getWeatherDesc().get(0).getValue() : "Unknown";
            
            System.out.println("Temperature: " + current.getTemp_C() + "°C / " + current.getTemp_F() + "°F");
            System.out.println("Conditions: " + weatherDesc);
            System.out.println("Humidity: " + current.getHumidity() + "%");
            System.out.println("Wind: " + current.getWindspeedKmph() + " km/h (" + current.getWinddir16Point() + ")");
        } else {
            System.out.println("No current conditions available.");
        }
    }
    
    //
    // Model classes for Weather API responses
    //
    
    /**
     * Main weather response class
     */
    public static class WeatherResponse {
        private List<CurrentCondition> current_condition;
        private List<NearestArea> nearest_area;
        private List<Weather> weather;
        
        public List<CurrentCondition> getCurrent_condition() {
            return current_condition;
        }
        
        public void setCurrent_condition(List<CurrentCondition> current_condition) {
            this.current_condition = current_condition;
        }
        
        public List<NearestArea> getNearest_area() {
            return nearest_area;
        }
        
        public void setNearest_area(List<NearestArea> nearest_area) {
            this.nearest_area = nearest_area;
        }
        
        public List<Weather> getWeather() {
            return weather;
        }
        
        public void setWeather(List<Weather> weather) {
            this.weather = weather;
        }
    }
    
    /**
     * Current weather conditions
     */
    public static class CurrentCondition {
        @JsonProperty("FeelsLikeC")
        private String FeelsLikeC;
        @JsonProperty("FeelsLikeF")
        private String FeelsLikeF;
        private String cloudcover;
        private String humidity;
        private String localObsDateTime;
        private String observation_time;
        private String precipInches;
        private String precipMM;
        private String pressure;
        private String pressureInches;
        private String temp_C;
        private String temp_F;
        private String uvIndex;
        private String visibility;
        private String visibilityMiles;
        private String weatherCode;
        private List<WeatherDesc> weatherDesc;
        private String winddir16Point;
        private String winddirDegree;
        private String windspeedKmph;
        private String windspeedMiles;
        
        public String getFeelsLikeC() {
            return FeelsLikeC;
        }
        
        public void setFeelsLikeC(String feelsLikeC) {
            this.FeelsLikeC = feelsLikeC;
        }
        
        public String getFeelsLikeF() {
            return FeelsLikeF;
        }
        
        public void setFeelsLikeF(String feelsLikeF) {
            this.FeelsLikeF = feelsLikeF;
        }
        
        public String getCloudcover() {
            return cloudcover;
        }
        
        public void setCloudcover(String cloudcover) {
            this.cloudcover = cloudcover;
        }
        
        public String getHumidity() {
            return humidity;
        }
        
        public void setHumidity(String humidity) {
            this.humidity = humidity;
        }
        
        public String getLocalObsDateTime() {
            return localObsDateTime;
        }
        
        public void setLocalObsDateTime(String localObsDateTime) {
            this.localObsDateTime = localObsDateTime;
        }
        
        public String getObservation_time() {
            return observation_time;
        }
        
        public void setObservation_time(String observation_time) {
            this.observation_time = observation_time;
        }
        
        public String getPrecipInches() {
            return precipInches;
        }
        
        public void setPrecipInches(String precipInches) {
            this.precipInches = precipInches;
        }
        
        public String getPrecipMM() {
            return precipMM;
        }
        
        public void setPrecipMM(String precipMM) {
            this.precipMM = precipMM;
        }
        
        public String getPressure() {
            return pressure;
        }
        
        public void setPressure(String pressure) {
            this.pressure = pressure;
        }
        
        public String getPressureInches() {
            return pressureInches;
        }
        
        public void setPressureInches(String pressureInches) {
            this.pressureInches = pressureInches;
        }
        
        public String getTemp_C() {
            return temp_C;
        }
        
        public void setTemp_C(String temp_C) {
            this.temp_C = temp_C;
        }
        
        public String getTemp_F() {
            return temp_F;
        }
        
        public void setTemp_F(String temp_F) {
            this.temp_F = temp_F;
        }
        
        public String getUvIndex() {
            return uvIndex;
        }
        
        public void setUvIndex(String uvIndex) {
            this.uvIndex = uvIndex;
        }
        
        public String getVisibility() {
            return visibility;
        }
        
        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }
        
        public String getVisibilityMiles() {
            return visibilityMiles;
        }
        
        public void setVisibilityMiles(String visibilityMiles) {
            this.visibilityMiles = visibilityMiles;
        }
        
        public String getWeatherCode() {
            return weatherCode;
        }
        
        public void setWeatherCode(String weatherCode) {
            this.weatherCode = weatherCode;
        }
        
        public List<WeatherDesc> getWeatherDesc() {
            return weatherDesc;
        }
        
        public void setWeatherDesc(List<WeatherDesc> weatherDesc) {
            this.weatherDesc = weatherDesc;
        }
        
        public String getWinddir16Point() {
            return winddir16Point;
        }
        
        public void setWinddir16Point(String winddir16Point) {
            this.winddir16Point = winddir16Point;
        }
        
        public String getWinddirDegree() {
            return winddirDegree;
        }
        
        public void setWinddirDegree(String winddirDegree) {
            this.winddirDegree = winddirDegree;
        }
        
        public String getWindspeedKmph() {
            return windspeedKmph;
        }
        
        public void setWindspeedKmph(String windspeedKmph) {
            this.windspeedKmph = windspeedKmph;
        }
        
        public String getWindspeedMiles() {
            return windspeedMiles;
        }
        
        public void setWindspeedMiles(String windspeedMiles) {
            this.windspeedMiles = windspeedMiles;
        }
        
        // Add debug method to print all field values
        @Override
        public String toString() {
            return "CurrentCondition{" +
                    "feelsLikeC='" + FeelsLikeC + '\'' +
                    ", feelsLikeF='" + FeelsLikeF + '\'' +
                    ", temp_C='" + temp_C + '\'' +
                    ", temp_F='" + temp_F + '\'' +
                    ", weatherDesc=" + (weatherDesc != null ? weatherDesc.size() : "null") +
                    '}';
        }
    }
    
    /**
     * Nearest area information
     */
    public static class NearestArea {
        private List<Value> areaName;
        private List<Value> country;
        private String latitude;
        private String longitude;
        private String population;
        private List<Value> region;
        
        public List<Value> getAreaName() {
            return areaName;
        }
        
        public void setAreaName(List<Value> areaName) {
            this.areaName = areaName;
        }
        
        public List<Value> getCountry() {
            return country;
        }
        
        public void setCountry(List<Value> country) {
            this.country = country;
        }
        
        public String getLatitude() {
            return latitude;
        }
        
        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }
        
        public String getLongitude() {
            return longitude;
        }
        
        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }
        
        public String getPopulation() {
            return population;
        }
        
        public void setPopulation(String population) {
            this.population = population;
        }
        
        public List<Value> getRegion() {
            return region;
        }
        
        public void setRegion(List<Value> region) {
            this.region = region;
        }
    }
    
    /**
     * Weather forecast information
     */
    public static class Weather {
        private List<Astronomy> astronomy;
        private String avgtempC;
        private String avgtempF;
        private String date;
        private String maxtempC;
        private String maxtempF;
        private String mintempC;
        private String mintempF;
        private String sunHour;
        private String totalSnow_cm;
        private String uvIndex;
        
        public List<Astronomy> getAstronomy() {
            return astronomy;
        }
        
        public void setAstronomy(List<Astronomy> astronomy) {
            this.astronomy = astronomy;
        }
        
        public String getAvgtempC() {
            return avgtempC;
        }
        
        public void setAvgtempC(String avgtempC) {
            this.avgtempC = avgtempC;
        }
        
        public String getAvgtempF() {
            return avgtempF;
        }
        
        public void setAvgtempF(String avgtempF) {
            this.avgtempF = avgtempF;
        }
        
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public String getMaxtempC() {
            return maxtempC;
        }
        
        public void setMaxtempC(String maxtempC) {
            this.maxtempC = maxtempC;
        }
        
        public String getMaxtempF() {
            return maxtempF;
        }
        
        public void setMaxtempF(String maxtempF) {
            this.maxtempF = maxtempF;
        }
        
        public String getMintempC() {
            return mintempC;
        }
        
        public void setMintempC(String mintempC) {
            this.mintempC = mintempC;
        }
        
        public String getMintempF() {
            return mintempF;
        }
        
        public void setMintempF(String mintempF) {
            this.mintempF = mintempF;
        }
        
        public String getSunHour() {
            return sunHour;
        }
        
        public void setSunHour(String sunHour) {
            this.sunHour = sunHour;
        }
        
        public String getTotalSnow_cm() {
            return totalSnow_cm;
        }
        
        public void setTotalSnow_cm(String totalSnow_cm) {
            this.totalSnow_cm = totalSnow_cm;
        }
        
        public String getUvIndex() {
            return uvIndex;
        }
        
        public void setUvIndex(String uvIndex) {
            this.uvIndex = uvIndex;
        }
    }
    
    /**
     * Astronomy information
     */
    public static class Astronomy {
        private String moon_illumination;
        private String moon_phase;
        private String moonrise;
        private String moonset;
        private String sunrise;
        private String sunset;
        
        public String getMoon_illumination() {
            return moon_illumination;
        }
        
        public void setMoon_illumination(String moon_illumination) {
            this.moon_illumination = moon_illumination;
        }
        
        public String getMoon_phase() {
            return moon_phase;
        }
        
        public void setMoon_phase(String moon_phase) {
            this.moon_phase = moon_phase;
        }
        
        public String getMoonrise() {
            return moonrise;
        }
        
        public void setMoonrise(String moonrise) {
            this.moonrise = moonrise;
        }
        
        public String getMoonset() {
            return moonset;
        }
        
        public void setMoonset(String moonset) {
            this.moonset = moonset;
        }
        
        public String getSunrise() {
            return sunrise;
        }
        
        public void setSunrise(String sunrise) {
            this.sunrise = sunrise;
        }
        
        public String getSunset() {
            return sunset;
        }
        
        public void setSunset(String sunset) {
            this.sunset = sunset;
        }
    }
    
    /**
     * Weather description
     */
    public static class WeatherDesc {
        private String value;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    /**
     * Generic value class for nested structures
     */
    public static class Value {
        private String value;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    /**
     * Simple pair class to hold a location name and its weather response
     */
    private static class Pair<F, S> {
        private final F first;
        private final S second;
        
        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
        
        public F getFirst() {
            return first;
        }
        
        public S getSecond() {
            return second;
        }
    }
} 