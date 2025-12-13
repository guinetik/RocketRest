# RocketRest Examples

This directory contains example applications demonstrating how to use the RocketRest library.

## Running Examples

All examples can be run through the main HelloWorld class, which provides an interactive menu to choose which example to run.

```bash
# First, make sure you have installed the RocketRest core library to your local Maven repository
cd ../rocketrest-core
mvn clean install

# Then run the examples
cd ../examples
mvn compile exec:java
```

## Available Examples

### 1. JSONPlaceholder Todo Example

This example demonstrates how to use RocketRest to make a simple GET request to an API endpoint and deserialize the response.

The example:
1. Creates a RocketRest client with a configuration pointing to the JSONPlaceholder API
2. Makes a GET request to the `/todos/1` endpoint
3. Deserializes the JSON response into a Todo object
4. Prints the Todo object to the console
5. Properly shuts down the client to release resources

Expected Output:

```
Fetching Todo from JSONPlaceholder API...
Successfully fetched Todo:
Todo{userId=1, id=1, title='delectus aut autem', completed=false}
```

### 2. Fake-JSON API Example

This example demonstrates how to make multiple GET requests to different API endpoints and handle the responses as raw strings without creating model classes.

The example:
1. Creates a RocketRest client with an empty base URL
2. Makes GET requests to three different endpoints:
   - https://fake-json-api.mock.beeceptor.com/users
   - https://fake-json-api.mock.beeceptor.com/companies
   - https://dummy-json.mock.beeceptor.com/posts
3. Processes each response as a raw String
4. Prints the responses to the console
5. Properly shuts down the client to release resources

### 3. Fluent API Example

This example demonstrates using RocketRest's fluent API and Result pattern for more functional error handling.

The example:
1. Creates a fluent API client
2. Makes a GET request using the fluent interface
3. Processes the Result object, which contains either a success value or an error
4. Demonstrates error handling without try-catch blocks

### 4. RocketRest Fluent Example

This example demonstrates RocketRest's fluent API for chaining multiple API calls in a readable way.

The example:
1. Creates a RocketRest client
2. Uses the fluent() method to get a fluent API interface
3. Makes multiple API calls with a functional programming style
4. Processes responses using map, flatMap, and other functional operators

### 5. Circuit Breaker Example

This example demonstrates the circuit breaker pattern implemented in RocketRest, which helps prevent cascading failures.

The example:
1. Sets up a client with circuit breaker configuration
2. Makes repeated API calls to an endpoint that sometimes fails
3. Shows how the circuit breaker opens after a certain number of failures
4. Demonstrates the circuit breaker automatically closing after a reset timeout
5. Illustrates how this pattern improves application resilience

### 6. NASA APOD Example

This example demonstrates accessing NASA's Astronomy Picture of the Day API.

The example:
1. Creates a RocketRest client with NASA API configuration
2. Makes a GET request to the NASA APOD API
3. Processes and displays the astronomy picture information
4. Shows working with external APIs that require API keys

### 7. PokéAPI Example

This example demonstrates working with the PokéAPI to retrieve Pokémon information.

The example:
1. Creates a RocketRest client for the PokéAPI
2. Makes requests to get information about different Pokémon
3. Deserializes complex nested JSON responses
4. Shows how to navigate API relationships by following URLs in responses

### 8. Weather API Example

This example demonstrates integration with a weather API service.

The example:
1. Creates a RocketRest client for a weather service
2. Makes requests to get weather data for a specific location
3. Processes and displays the weather information
4. Shows working with query parameters and API keys

### 9. SAP IDP Example

This example demonstrates authentication with SAP Identity Provider (IDP) to access SAP APIs.

The example:
1. Sets up OAuth2 assertion authentication for SAP
2. Creates an authentication strategy using SAP IDP
3. Refreshes the OAuth token and prints it
4. Makes an authenticated API call to SAP's API
5. Demonstrates environment variable configuration via:
   - SAP_CLIENT_ID
   - SAP_USER_ID
   - SAP_PRIVATE_KEY
   - SAP_COMPANY_ID
   - SAP_GRANT_TYPE
   - SAP_IDP_URL
   - SAP_TOKEN_URL
   - SAP_BASE_URL

### 10. ADP API Example

This example demonstrates secure integration with ADP's API using custom SSL certificates and OAuth2.

The example:
1. Sets up SSL/TLS with a custom PKCS12 certificate
2. Configures OAuth2 client credentials authentication
3. Makes a secure API call to ADP's API
4. Shows SSL context configuration with RocketSSL
5. Demonstrates environment variable configuration via:
   - ADP_CLIENT_ID
   - ADP_CLIENT_SECRET
   - ADP_CERTIFICATE_FILE
   - ADP_CERTIFICATE_PASSWORD

## Adding New Examples

To add a new example:

1. Create a new class that implements the `Example` interface
2. Implement the `getName()` and `run()` methods
3. Add the example to the `EXAMPLES` list in `HelloWorld.java` 