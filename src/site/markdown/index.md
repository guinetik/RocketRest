# RocketRest

RocketRest is a lightweight Java 8 library for making HTTP API requests with a clean, fluent interface. It provides both synchronous and asynchronous APIs, abstracting away the underlying HTTP client implementation.

## Features

- **Minimal Dependencies** - Only Jackson for JSON and SLF4J for logging
- **Default HttpURLConnection** - Vanilla implementation with no external HTTP libraries required
- **Abstract Architecture** - Allows different HTTP client implementations
- **Sync & Async Support** - CompletableFuture-based async operations
- **Automatic JSON Handling** - Built-in serialization/deserialization
- **Fluent API** - Clean builder pattern for requests (GET, POST, PUT, DELETE)
- **Result Pattern** - Elegant error handling without exceptions
- **Pluggable Auth** - Multiple authentication strategies out of the box
- **Circuit Breaker** - Built-in resilience patterns
- **Mock Support** - Comprehensive testing utilities

## Quick Start

Add RocketRest to your Maven project:

```xml
<dependency>
    <groupId>com.guinetik</groupId>
    <artifactId>rocketrest-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Make your first API call:

```java
// Create a simple client
RocketRest client = new RocketRest("https://api.example.com");

// Make a GET request
User user = client.get("/users/1", User.class);
System.out.println("Hello, " + user.getName());

// Don't forget to shutdown when done
client.shutdown();
```

## Usage Example

```java
// Create configuration with default options
RocketRestConfig config = RocketRestConfig.builder("https://api.example.com")
        .authStrategy(AuthStrategyFactory.createBearerToken("your-api-token"))
        .defaultOptions(options -> {
            options.set(RocketRestOptions.RETRY_ENABLED, true);
            options.set(RocketRestOptions.MAX_RETRIES, 3);
            options.set(RocketRestOptions.LOG_REQUEST_BODY, true);
        })
        .build();

// Create RocketRest client (inherits default options from config)
RocketRest client = new RocketRest(config);

try {
    // Synchronous API
    User user = client.sync().get("/users/1", User.class);

    // Asynchronous API with CompletableFuture
    CompletableFuture<User> futureUser = client.async().get("/users/2", User.class);
    futureUser.thenAccept(u -> System.out.println("Received user: " + u.getName()));

    // Fluent API with Result pattern for elegant error handling
    Result<User, ApiError> result = client.fluent().get("/users/3", User.class);
    result.match(
        user -> System.out.println("Success: " + user.getName()),
        error -> System.err.println("Error: " + error.getMessage())
    );

} finally {
    // Shutdown the client to release resources
    client.shutdown();
}
```

## Modules

| Module | Description |
|--------|-------------|
| [rocketrest-core](rocketrest-core/index.html) | Core library with HTTP client, auth strategies, and utilities |
| [rocketrest-examples](rocketrest-examples/index.html) | Example applications demonstrating library usage |

## API Documentation

See the [Javadoc](apidocs/index.html) for complete API documentation.

## Source Code

The source code is available on [GitHub](https://github.com/guinetik/rocketrest).
