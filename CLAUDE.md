# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a microservices-based Spring Boot application that demonstrates event-driven architecture using Kafka for asynchronous communication and Redis for caching. The system fetches news data from MediaStack API based on date requests.

**Architecture Pattern**: Event-Driven Microservices with Cache-Aside Pattern

### Services

1. **news-api** (Port 8080): REST API service that:
   - Exposes GET endpoint `/api/v1/news?date=YYYY-MM-DD` for news retrieval
   - Checks Redis cache first for requested date
   - If cache miss, publishes date to Kafka topic "news" and returns 404
   - Uses reactive programming with Spring WebFlux

2. **worker-service** (Port 8081): Background worker that:
   - Listens to Kafka topic "news" for date requests
   - Fetches news from MediaStack external API for the requested date
   - Caches the response in Redis with date as key
   - Handles external API errors with custom exception handling

### Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.7
- **Build Tool**: Gradle (multi-module project)
- **Message Broker**: Apache Kafka with Zookeeper
- **Cache**: Redis
- **Reactive Stack**: Spring WebFlux, Reactor
- **External API**: MediaStack News API

## Build and Run Commands

### Build the project
```bash
# Build all modules from root
./gradlew build

# Build specific module
./gradlew :news-api:build
./gradlew :worker-service:build

# Clean and build
./gradlew clean build
```

### Run tests
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :news-api:test
./gradlew :worker-service:test
```

### Docker operations
```bash
# Build and start all services (Zookeeper, Kafka, Redis, news-api, worker-service)
docker-compose up --build

# Start in detached mode
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f
docker-compose logs -f news-api
docker-compose logs -f worker-service
```

### Local development
```bash
# Run news-api locally (requires Kafka and Redis running)
cd news-api
../gradlew bootRun

# Run worker-service locally
cd worker-service
../gradlew bootRun
```

## Architecture Details

### Message Flow

1. Client requests news via GET `/api/v1/news?date=2024-01-15`
2. `NewsController` validates date format (YYYY-MM-DD)
3. `NewsService` checks Redis cache via `NewsRepository`
4. **Cache Hit**: Returns data immediately with 200 OK
5. **Cache Miss**:
   - Publishes date to Kafka topic "news"
   - Returns 404 with message "Data not found, sending request to broker"
6. `KafkaListeners` in worker-service consumes the message
7. `MediaStackService` calls external API with date parameter
8. Worker stores response in Redis
9. Subsequent requests for same date return cached data

### Kafka Configuration

- **Topic**: "news" (created automatically via `KafkaTopicConfig`)
- **Producer**: In news-api service
- **Consumer**: In worker-service with group "newsgroup" (see `Constants.MESSAGE_GROUP_NAME`)
- **Bootstrap Servers**: Configured via environment variable `KAFKA_SERVER`

### Redis Configuration

- **Connection**: Configured via environment variables (REDIS_SERVER, REDIS_PORT, REDIS_PASSWORD)
- **Client**: Spring Data Redis Reactive
- **Key Pattern**: Date strings in YYYY-MM-DD format
- **Value**: JSON response from MediaStack API

### Environment Variables

Both services require these variables (set in docker-compose.yml):
- `KAFKA_SERVER`: Kafka bootstrap server address
- `REDIS_SERVER`: Redis host
- `REDIS_PORT`: Redis port
- `REDIS_PASSWORD`: Redis password

Worker-service additionally requires:
- `MEDIASTACK_URI`: MediaStack API endpoint
- `MEDIASTACK_API_KEY`: API key for MediaStack

### Error Handling

- **news-api**: Uses `GlobalControllerAdvice` for centralized exception handling
- **worker-service**: Custom `ExternalApiException` for API errors with status code and response body
- Validation errors for date format return appropriate error responses via `ErrorCatalog` and `ErrorResponse`

## Important Code Patterns

### Reactive Programming
All services use Project Reactor (Mono/Flux). When adding new features:
- Use `Mono<T>` for single values
- Use `Flux<T>` for streams
- Chain operations with `.flatMap()`, `.map()`, `.switchIfEmpty()`, etc.
- Subscribe at the edge (controllers or listeners)

### Repository Pattern
Both services implement repository pattern with interface + impl:
- `NewsRepository` interface defines contract
- `NewsRepositoryImpl` contains Redis operations using `ReactiveRedisTemplate`

### Configuration Classes
- `KafkaProducerConfig` / `KafkaConsumerConfig`: Kafka setup
- `KafkaTopicConfig`: Topic creation
- `RedisConfig`: Redis connection
- `WebClientConfig`: External API client (worker-service)

## Module Structure

```
spring-kafka-redis-root/
├── news-api/              # REST API service
│   ├── config/            # Spring configurations
│   ├── controller/        # REST controllers
│   ├── service/           # Business logic
│   ├── repository/        # Data access layer
│   ├── models/dto/        # DTOs and response objects
│   └── utils/             # Constants and utilities
├── worker-service/        # Kafka consumer service
│   ├── config/            # Spring configurations
│   ├── listener/          # Kafka listeners
│   ├── service/           # External API integration
│   ├── repository/        # Redis operations
│   └── model/             # Domain models and exceptions
└── docker-compose.yml     # Container orchestration
```

## Testing Endpoints

```bash
# Request news for a specific date
curl "http://localhost:8080/api/v1/news?date=2024-01-15"

# First request will return 404 and trigger async fetch
# Wait a few seconds for worker to process
# Second request will return cached data with 200
```

## Development Notes

- The project uses Lombok - ensure annotation processing is enabled in your IDE
- Date format must be YYYY-MM-DD (validated via regex pattern in `Constants.DATE_FORMAT`)
- The topic name "news" is defined in `Constants.TOPIC_NAME` in both services
- Worker service uses reactive WebClient for non-blocking external API calls
- Both services must be running for the complete flow to work