# Spring Boot Kafka Redis Microservices

A microservices architecture demonstrating event-driven communication using Apache Kafka, Redis caching, and Spring Boot with reactive programming.

## Architecture Overview

This project implements a cache-aside pattern with asynchronous data fetching through Kafka messaging. The system includes two main flows:

1. **News Flow**: Fetches news data from MediaStack API on demand
2. **Crypto Flow**: Scheduled fetching of cryptocurrency prices from CoinGecko API

### Architecture Diagram

![Architecture Diagram](architecture.png)

<details>
<summary>View Mermaid Source (click to expand)</summary>

```mermaid
graph TB
    Client[Client/Browser]

    subgraph "Docker Environment"
        subgraph "News Flow"
            NewsAPI[news-api:8080<br/>REST API Service]
            Worker[worker-service:8081<br/>Kafka Consumer]
        end

        subgraph "Crypto Flow"
            CryptoFetcher[crypto-fetcher-service:8083<br/>Scheduled Producer]
        end

        subgraph "Infrastructure"
            Kafka[Apache Kafka<br/>Message Broker]
            Zookeeper[Zookeeper<br/>Kafka Coordinator]
            Redis[(Redis Cache<br/>Port 6379)]
            KafkaUI[Kafka UI:8090<br/>Monitoring]
        end

        Kafka -->|depends on| Zookeeper
        KafkaUI -->|monitors| Kafka
    end

    MediaStack[MediaStack API<br/>External News Service]
    CoinGecko[CoinGecko API<br/>Crypto Prices]

    %% News Flow
    Client -->|1. GET /api/v1/news?date=YYYY-MM-DD| NewsAPI
    NewsAPI -->|2. Check cache| Redis
    Redis -->|3a. Cache HIT| NewsAPI
    NewsAPI -->|3b. Cache MISS<br/>Publish to 'news' topic| Kafka
    Kafka -->|4. Consume message| Worker
    Worker -->|5. Fetch news| MediaStack
    Worker -->|6. Store in cache| Redis

    %% Crypto Flow
    CryptoFetcher -->|Scheduled every 5min| CoinGecko
    CoinGecko -->|Return prices| CryptoFetcher
    CryptoFetcher -->|Publish to 'crypto-prices' topic| Kafka

    style NewsAPI fill:#4CAF50,stroke:#333,stroke-width:2px,color:#fff
    style Worker fill:#2196F3,stroke:#333,stroke-width:2px,color:#fff
    style CryptoFetcher fill:#9C27B0,stroke:#333,stroke-width:2px,color:#fff
    style Kafka fill:#FF9800,stroke:#333,stroke-width:2px,color:#fff
    style Redis fill:#F44336,stroke:#333,stroke-width:2px,color:#fff
    style KafkaUI fill:#00BCD4,stroke:#333,stroke-width:2px,color:#fff
```

</details>

---

## Crypto Tracker Tutorial Progress

This project includes a learning tutorial for building a Crypto Price Tracker. See [CRYPTO_TRACKER_TUTORIAL.md](CRYPTO_TRACKER_TUTORIAL.md) for the complete guide.

### Phase 1: Crypto Fetcher Service - COMPLETED

- [x] Create module `crypto-fetcher-service`
- [x] Configure CoinGecko API client with WebClient
- [x] Implement scheduled job with `@Scheduled`
- [x] Create topic `crypto-prices`
- [x] Publish prices to Kafka with message keys
- [x] Verify messages in Kafka UI

### Phase 2: Price Processor Service - PENDING

- [ ] Create module `price-processor-service`
- [ ] Configure Kafka consumer
- [ ] Implement Redis storage logic
- [ ] Calculate statistics (min, max, avg)

### Phase 3: Alert Service - PENDING

- [ ] Create module `alert-service`
- [ ] Implement change detection logic
- [ ] Create topic `price-alerts`
- [ ] Publish alerts

### Phase 4: Crypto API - PENDING

- [ ] Create module `crypto-api`
- [ ] Endpoints for current prices
- [ ] Endpoints for history
- [ ] Endpoints for alerts

---

## Components

### 1. news-api (Port 8080)
**REST API Service** - Handles client requests and manages cache

- **Technology**: Spring Boot 3.5.7, Spring WebFlux (Reactive)
- **Role**: Producer service
- **Responsibilities**:
  - Exposes REST endpoint for news retrieval
  - Validates date format (YYYY-MM-DD)
  - Checks Redis cache for requested data
  - Publishes messages to Kafka on cache miss
  - Returns cached data on cache hit

### 2. worker-service (Port 8081)
**Background Worker** - Processes Kafka messages and fetches external data

- **Technology**: Spring Boot 3.5.7, Spring WebFlux, WebClient
- **Role**: Consumer service
- **Responsibilities**:
  - Listens to Kafka topic "news"
  - Fetches news from MediaStack API
  - Stores fetched data in Redis cache
  - Handles external API errors gracefully

### 3. crypto-fetcher-service (Port 8083)
**Scheduled Producer** - Fetches crypto prices and publishes to Kafka

- **Technology**: Spring Boot 3.5.7, Spring WebFlux, WebClient
- **Role**: Scheduled producer service
- **Responsibilities**:
  - Fetches prices from CoinGecko API every 5 minutes
  - Publishes to Kafka topic "crypto-prices"
  - Uses message keys (BTC, ETH, SOL) for partitioning
  - Tracks Bitcoin, Ethereum, and Solana prices

### 4. Apache Kafka + Zookeeper
**Message Broker** - Enables asynchronous communication

- **Image**: confluentinc/cp-kafka:7.5.0
- **Topics**:
  - `news` - News date requests
  - `crypto-prices` - Cryptocurrency price updates
- **Purpose**: Decouples services and enables async processing

### 5. Redis
**Cache Layer** - Stores data for fast retrieval

- **Image**: redis:latest
- **Purpose**: Fast data retrieval and reduced external API calls
- **Key Patterns**:
  - News: Date strings (YYYY-MM-DD)
  - Crypto: `crypto:current:{symbol}`, `crypto:stats:{symbol}`

### 6. Kafka UI (Port 8090)
**Monitoring Dashboard** - Visual interface for Kafka

- **Image**: provectuslabs/kafka-ui:latest
- **Purpose**: Monitor topics, messages, consumers, and brokers
- **Access**: http://localhost:8090

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Spring Boot | 3.5.7 | Application framework |
| Spring WebFlux | - | Reactive web framework |
| Spring Kafka | - | Kafka integration |
| Spring Data Redis | - | Redis integration |
| Project Reactor | - | Reactive programming |
| Gradle | - | Build tool |
| Docker | - | Containerization |
| Apache Kafka | 7.5.0 | Message broker |
| Redis | latest | Caching layer |
| Kafka UI | latest | Kafka monitoring |
| MediaStack API | v1 | External news API |
| CoinGecko API | v3 | Cryptocurrency prices API |

## Communication Flow

### News Flow - First Request (Cache Miss)
```
1. Client → GET /api/v1/news?date=2024-01-15
2. news-api → Check Redis cache
3. Redis → Cache MISS (no data found)
4. news-api → Publish "2024-01-15" to Kafka topic "news"
5. news-api → Return 404 with message "Data not found, sending request to broker"
6. worker-service → Consume message from Kafka
7. worker-service → Call MediaStack API with date parameter
8. MediaStack API → Return news data
9. worker-service → Store data in Redis with key "2024-01-15"
```

### News Flow - Subsequent Requests (Cache Hit)
```
1. Client → GET /api/v1/news?date=2024-01-15
2. news-api → Check Redis cache
3. Redis → Cache HIT (data found)
4. news-api → Return 200 OK with cached news data
```

### Crypto Flow - Scheduled Fetch
```
1. Scheduler triggers every 5 minutes
2. crypto-fetcher-service → Call CoinGecko API
3. CoinGecko API → Return BTC, ETH, SOL prices
4. crypto-fetcher-service → Publish each price to Kafka topic "crypto-prices"
   - Key: Symbol (BTC, ETH, SOL)
   - Value: CryptoPrice object
5. Messages available for consumers (price-processor, alert-service)
```

## Prerequisites

- Docker and Docker Compose
- Java 17 or higher
- Gradle 8.x

## Getting Started

### 1. Clone the repository
```bash
git clone <repository-url>
cd spring-kafka-redis-root
```

### 2. Build the project
```bash
./gradlew clean build -x test
```

### 3. Start all services with Docker Compose
```bash
docker-compose up -d --build
```

### 4. Verify services are running
```bash
docker-compose ps
```

You should see all containers running:
- zookeeper
- kafka
- kafka-ui
- redis
- news-api
- worker-service
- crypto-fetcher-service

### 5. Access Kafka UI
Open http://localhost:8090 to monitor:
- Topics and their messages
- Consumer groups
- Broker status

### 6. Check logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f news-api
docker-compose logs -f worker-service
docker-compose logs -f crypto-fetcher-service
```

## API Endpoints

### News API

#### Get News by Date
**Endpoint**: `GET /api/v1/news?date={date}`

**Parameters**:
- `date` (required): Date in format YYYY-MM-DD

**Example Request**:
```bash
curl "http://localhost:8080/api/v1/news?date=2024-01-15"
```

**First Request Response** (Cache Miss - 404):
```json
{
  "message": "Data not found, sending request to broker",
  "status": false,
  "data": null
}
```

**Subsequent Request Response** (Cache Hit - 200):
```json
{
  "message": "Data found",
  "status": true,
  "data": {
    "pagination": {...},
    "data": [
      {
        "author": "John Doe",
        "title": "Breaking News...",
        "description": "...",
        "url": "...",
        "source": "...",
        "category": "general",
        "published_at": "2024-01-15T10:00:00+00:00"
      }
    ]
  }
}
```

### Crypto Fetcher Service

The crypto-fetcher-service runs automatically and publishes prices to Kafka every 5 minutes. You can monitor the messages in Kafka UI at http://localhost:8090.

**Message Format in topic `crypto-prices`**:
```json
{
  "symbol": "BTC",
  "name": "Bitcoin",
  "priceUsd": 43250.50,
  "priceChange24h": 2.5,
  "marketCap": 840000000000,
  "timestamp": "2024-01-15T10:00:00Z"
}
```

## Environment Variables

### news-api
| Variable | Description | Default |
|----------|-------------|---------|
| KAFKA_SERVER | Kafka bootstrap server | kafka:29092 |
| REDIS_SERVER | Redis host | redis |
| REDIS_PORT | Redis port | 6379 |
| REDIS_PASSWORD | Redis password | myredis |

### worker-service
| Variable | Description | Default |
|----------|-------------|---------|
| KAFKA_SERVER | Kafka bootstrap server | kafka:29092 |
| REDIS_SERVER | Redis host | redis |
| REDIS_PORT | Redis port | 6379 |
| REDIS_PASSWORD | Redis password | myredis |
| MEDIASTACK_URI | MediaStack API endpoint | http://api.mediastack.com/v1/news |
| MEDIASTACK_API_KEY | MediaStack API key | (configured in docker-compose.yml) |

### crypto-fetcher-service
| Variable | Description | Default |
|----------|-------------|---------|
| KAFKA_SERVER | Kafka bootstrap server | kafka:29092 |

## Project Structure

```
spring-kafka-redis-root/
├── news-api/                        # REST API Service
│   ├── src/main/java/
│   │   └── com/alexlondon07/news_api/
│   │       ├── config/              # Kafka, Redis configs
│   │       ├── controller/          # REST controllers
│   │       ├── service/             # Business logic
│   │       ├── repository/          # Redis operations
│   │       ├── models/              # DTOs and responses
│   │       └── utils/               # Constants and utilities
│   ├── Dockerfile
│   └── build.gradle
│
├── worker-service/                  # Kafka Consumer Service
│   ├── src/main/java/
│   │   └── com/alexlondon07/worker_service/
│   │       ├── config/              # Kafka, Redis, WebClient configs
│   │       ├── listener/            # Kafka listeners
│   │       ├── service/             # MediaStack integration
│   │       ├── repository/          # Redis operations
│   │       ├── model/               # Domain models
│   │       └── utils/               # Constants
│   ├── Dockerfile
│   └── build.gradle
│
├── crypto-fetcher-service/          # Scheduled Crypto Producer
│   ├── src/main/java/
│   │   └── com/alexlondon07/crypto_fetcher_service/
│   │       ├── config/              # Kafka, WebClient configs
│   │       ├── scheduler/           # Scheduled jobs
│   │       ├── service/             # CoinGecko integration
│   │       ├── model/               # CryptoPrice DTO
│   │       └── utils/               # Constants
│   ├── Dockerfile
│   └── build.gradle
│
├── docker-compose.yml               # Container orchestration
├── build.gradle                     # Root build configuration
├── settings.gradle                  # Multi-module setup
├── CLAUDE.md                        # AI assistant instructions
├── CRYPTO_TRACKER_TUTORIAL.md       # Learning tutorial
└── README.md                        # This file
```

## Development

### Build specific module
```bash
./gradlew :news-api:build
./gradlew :worker-service:build
./gradlew :crypto-fetcher-service:build
```

### Run tests
```bash
./gradlew test
```

### Run locally (without Docker)
Make sure Kafka, Zookeeper, and Redis are running, then:

```bash
# Terminal 1 - news-api
cd news-api
../gradlew bootRun

# Terminal 2 - worker-service
cd worker-service
../gradlew bootRun

# Terminal 3 - crypto-fetcher-service
cd crypto-fetcher-service
../gradlew bootRun
```

### Clean and rebuild
```bash
./gradlew clean build
docker-compose down
docker-compose up -d --build
```

## Key Features

- **Reactive Programming**: Non-blocking I/O with Spring WebFlux and Project Reactor
- **Event-Driven Architecture**: Decoupled services communicating via Kafka
- **Cache-Aside Pattern**: Redis caching for improved performance
- **Scheduled Jobs**: Automatic crypto price fetching with `@Scheduled`
- **Message Keys**: Kafka partitioning for ordered processing per symbol
- **Asynchronous Processing**: Background workers handle heavy operations
- **Monitoring**: Kafka UI for real-time message visualization
- **Error Handling**: Comprehensive error handling for external API failures
- **Validation**: Request validation with Jakarta Validation
- **Containerization**: Fully containerized with Docker Compose
- **Scalability**: Kafka consumer groups allow horizontal scaling

## Troubleshooting

### Kafka connection errors
```bash
# Check if Kafka is healthy
docker-compose logs kafka

# Restart Kafka
docker-compose restart kafka
```

### Redis connection errors
```bash
# Check Redis logs
docker-compose logs redis

# Test Redis connection
docker exec -it spring-kafka-redis-root-redis-1 redis-cli
> AUTH myredis
> PING
```

### Service not starting
```bash
# Check service logs
docker-compose logs news-api
docker-compose logs worker-service
docker-compose logs crypto-fetcher-service

# Rebuild from scratch
docker-compose down -v
./gradlew clean build
docker-compose up -d --build
```

### Verify Kafka topics
```bash
# Access Kafka UI
open http://localhost:8090

# Or use CLI
docker exec -it <kafka-container> kafka-topics --list --bootstrap-server localhost:9092
```

### Port already in use
```bash
# Stop all containers
docker-compose down

# Check what's using the port
lsof -i :8080
lsof -i :8081
lsof -i :8083
lsof -i :8090

# Kill the process or change port in docker-compose.yml
```

## Stopping the Application

```bash
# Stop all containers
docker-compose down

# Stop and remove volumes (clears Redis data)
docker-compose down -v
```

## Future Enhancements

- [ ] **Phase 2**: Price Processor Service - Store prices and calculate stats
- [ ] **Phase 3**: Alert Service - Detect significant price changes
- [ ] **Phase 4**: Crypto API - REST endpoints for crypto data
- [ ] Add authentication and authorization
- [ ] Implement circuit breaker pattern for external API calls
- [ ] Add metrics and monitoring (Prometheus, Grafana)
- [ ] Implement distributed tracing (Zipkin, Jaeger)
- [ ] Add comprehensive unit and integration tests
- [ ] Implement API rate limiting
- [ ] Add API documentation (Swagger/OpenAPI)
- [ ] Implement Kafka Streams for real-time processing
- [ ] Add WebSocket for live price updates
- [ ] Add Kubernetes deployment manifests

## License

This project is for educational purposes.

## Author

Alex London