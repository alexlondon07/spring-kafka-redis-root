# Crypto Price Tracker - Tutorial Paso a Paso

Este tutorial te guiará en la implementación de un sistema de tracking de criptomonedas usando Kafka. Aprenderás conceptos avanzados mientras construyes un proyecto real.

---

## Tabla de Contenidos

1. [Arquitectura del Sistema](#1-arquitectura-del-sistema)
2. [Conceptos de Kafka que Aprenderás](#2-conceptos-de-kafka-que-aprenderás)
3. [Plan de Implementación](#3-plan-de-implementación)
4. [Fase 1: Crypto Fetcher Service](#fase-1-crypto-fetcher-service)
5. [Fase 2: Price Processor Service](#fase-2-price-processor-service)
6. [Fase 3: Alert Service](#fase-3-alert-service)
7. [Fase 4: Crypto API](#fase-4-crypto-api)
8. [Ejercicios Adicionales](#ejercicios-adicionales)

---

## 1. Arquitectura del Sistema

### Diagrama General

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CRYPTO PRICE TRACKER                            │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────┐     ┌──────────────────┐
│   CoinGecko  │     │   Scheduler │     │    crypto-api    │
│     API      │     │  (cada 5m)  │     │   (Port 8082)    │
└──────┬───────┘     └──────┬──────┘     └────────┬─────────┘
       │                    │                     │
       │   ┌────────────────┴──────────┐          │
       │   │                           │          │
       ▼   ▼                           │          │
┌──────────────────┐                   │          │
│ crypto-fetcher   │                   │          │
│   (Port 8083)    │                   │          │
│                  │                   │          │
│ - Fetch prices   │                   │          │
│ - Scheduled job  │                   │          │
└────────┬─────────┘                   │          │
         │                             │          │
         ▼                             │          │
┌─────────────────────────┐            │          │
│  Topic: crypto-prices   │◄───────────┘          │
│                         │                       │
│  Key: BTC, ETH, etc     │                       │
│  Value: {price, time}   │                       │
└────────┬────────────────┘                       │
         │                                        │
         ├────────────────────┐                   │
         ▼                    ▼                   │
┌─────────────────┐  ┌─────────────────┐          │
│ price-processor │  │  alert-service  │          │
│  (Consumer 1)   │  │  (Consumer 2)   │          │
│                 │  │                 │          │
│ - Guardar en    │  │ - Detectar      │          │
│   Redis         │  │   cambios >5%   │          │
│ - Calcular      │  │ - Generar       │          │
│   estadísticas  │  │   alertas       │          │
└────────┬────────┘  └────────┬────────┘          │
         │                    │                   │
         ▼                    ▼                   │
┌─────────────┐      ┌─────────────────┐          │
│    Redis    │      │ Topic: price-   │          │
│             │      │ alerts          │          │
│ - Precios   │      └────────┬────────┘          │
│ - History   │               │                   │
│ - Stats     │               ▼                   │
└──────┬──────┘      ┌─────────────────┐          │
       │             │  notification-  │          │
       │             │  service        │          │
       │             │  (log alerts)   │          │
       │             └─────────────────┘          │
       │                                          │
       └──────────────────────────────────────────┘
                    (consulta datos)
```

### Flujo de Datos

1. **Scheduled Fetch**: Cada 5 minutos, `crypto-fetcher` obtiene precios de CoinGecko
2. **Publish**: Publica cada precio al topic `crypto-prices`
3. **Multiple Consumers**:
   - `price-processor`: Guarda en Redis y calcula estadísticas
   - `alert-service`: Detecta cambios significativos
4. **Alerts**: Si hay cambio >5%, publica a `price-alerts`
5. **API**: `crypto-api` expone endpoints para consultar datos de Redis

### Nuevos Servicios

| Servicio | Puerto | Responsabilidad |
|----------|--------|-----------------|
| crypto-api | 8082 | REST API para consultar precios y alertas |
| crypto-fetcher | 8083 | Scheduled job que obtiene precios de CoinGecko |
| price-processor | 8084 | Procesa precios y guarda en Redis |
| alert-service | 8085 | Detecta cambios y genera alertas |

### Topics de Kafka

| Topic | Key | Value | Productores | Consumidores |
|-------|-----|-------|-------------|--------------|
| crypto-prices | symbol (BTC) | CryptoPrice DTO | crypto-fetcher | price-processor, alert-service |
| price-alerts | symbol | PriceAlert DTO | alert-service | notification-service (futuro) |

---

## 2. Conceptos de Kafka que Aprenderás

### Fase 1: Scheduled Producer
- **Qué es**: Un productor que envía mensajes periódicamente
- **Por qué importa**: Diferente al patrón request-response de tu proyecto actual
- **Concepto nuevo**: `@Scheduled` + KafkaTemplate

### Fase 2: Consumer Groups
- **Qué es**: Múltiples consumidores procesando el mismo topic
- **Por qué importa**: Permite escalabilidad horizontal
- **Concepto nuevo**: `group-id` y particiones

### Fase 3: Message Keys
- **Qué es**: Identificador que determina la partición
- **Por qué importa**: Garantiza orden de mensajes por key
- **Concepto nuevo**: Partitioning y ordering

### Fase 4: Multiple Topics
- **Qué es**: Un servicio publicando a un topic y otro consumiendo
- **Por qué importa**: Event-driven architecture real
- **Concepto nuevo**: Topic chaining

---

## 3. Plan de Implementación

### Fase 1: Crypto Fetcher Service (Semana 1)
**Objetivo**: Crear un servicio que obtenga precios cada 5 minutos

- [ ] Crear módulo `crypto-fetcher`
- [ ] Configurar CoinGecko API client
- [ ] Implementar scheduled job
- [ ] Crear topic `crypto-prices`
- [ ] Publicar precios a Kafka
- [ ] **Checkpoint**: Ver mensajes en Kafka

### Fase 2: Price Processor Service (Semana 2)
**Objetivo**: Consumir precios y guardar en Redis

- [ ] Crear módulo `price-processor`
- [ ] Configurar Kafka consumer
- [ ] Implementar lógica de almacenamiento en Redis
- [ ] Calcular estadísticas (min, max, avg)
- [ ] **Checkpoint**: Datos en Redis

### Fase 3: Alert Service (Semana 3)
**Objetivo**: Detectar cambios significativos y generar alertas

- [ ] Crear módulo `alert-service`
- [ ] Implementar lógica de detección de cambios
- [ ] Crear topic `price-alerts`
- [ ] Publicar alertas
- [ ] **Checkpoint**: Alertas generadas

### Fase 4: Crypto API (Semana 4)
**Objetivo**: Exponer datos via REST API

- [ ] Crear módulo `crypto-api`
- [ ] Endpoints para precios actuales
- [ ] Endpoints para histórico
- [ ] Endpoints para alertas
- [ ] **Checkpoint**: API funcionando

---

## Fase 1: Crypto Fetcher Service

### Objetivo de Aprendizaje
Aprenderás a crear un **Scheduled Producer** que obtiene datos de una API externa y los publica a Kafka periódicamente.

### Paso 1.1: Crear la Estructura del Módulo

Crea la siguiente estructura de carpetas:

```
crypto-fetcher/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── espejo/
│       │           └── cryptofetcher/
│       │               ├── CryptoFetcherApplication.java
│       │               ├── config/
│       │               │   ├── KafkaProducerConfig.java
│       │               │   ├── KafkaTopicConfig.java
│       │               │   └── WebClientConfig.java
│       │               ├── service/
│       │               │   ├── CoinGeckoService.java
│       │               │   └── PricePublisherService.java
│       │               ├── scheduler/
│       │               │   └── PriceFetchScheduler.java
│       │               ├── model/
│       │               │   └── CryptoPrice.java
│       │               └── utils/
│       │                   └── Constants.java
│       └── resources/
│           └── application.yml
├── build.gradle
└── Dockerfile
```

### Paso 1.2: Configurar build.gradle

Crea `crypto-fetcher/build.gradle`:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.espejo'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### Paso 1.3: Actualizar settings.gradle

Añade el nuevo módulo a `settings.gradle` en la raíz:

```gradle
rootProject.name = 'spring-kafka-redis-root'

include 'news-api'
include 'worker-service'
include 'crypto-fetcher'  // NUEVO
```

### Paso 1.4: Crear el Modelo CryptoPrice

Crea `model/CryptoPrice.java`:

```java
package com.espejo.cryptofetcher.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoPrice {
    private String symbol;           // BTC, ETH, etc.
    private String name;             // Bitcoin, Ethereum
    private BigDecimal priceUsd;     // Precio en USD
    private BigDecimal priceChange24h; // Cambio % en 24h
    private BigDecimal marketCap;    // Market cap
    private Instant timestamp;       // Momento de la consulta
}
```

**Pregunta para ti**: ¿Por qué usamos `BigDecimal` en lugar de `double` para precios?

<details>
<summary>Ver respuesta</summary>

`BigDecimal` evita errores de precisión en cálculos financieros. `double` puede tener errores como `0.1 + 0.2 = 0.30000000000000004`.

</details>

### Paso 1.5: Crear Constants

Crea `utils/Constants.java`:

```java
package com.espejo.cryptofetcher.utils;

public class Constants {

    // Kafka Topics
    public static final String TOPIC_CRYPTO_PRICES = "crypto-prices";

    // CoinGecko API
    public static final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3";

    // Cryptocurrencies to track
    public static final String[] CRYPTO_IDS = {"bitcoin", "ethereum", "solana"};

    // Símbolos para keys de Kafka
    public static final String BTC = "BTC";
    public static final String ETH = "ETH";
    public static final String SOL = "SOL";

    private Constants() {}
}
```

### Paso 1.6: Configurar application.yml

Crea `resources/application.yml`:

```yaml
server:
  port: 8083

spring:
  application:
    name: crypto-fetcher

  kafka:
    bootstrap-servers: ${KAFKA_SERVER:localhost:29092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false

# Configuración del scheduler
scheduler:
  fetch-interval: 300000  # 5 minutos en milliseconds

logging:
  level:
    com.espejo: DEBUG
    org.apache.kafka: WARN
```

### Paso 1.7: Crear KafkaProducerConfig

Crea `config/KafkaProducerConfig.java`:

```java
package com.espejo.cryptofetcher.config;

import com.espejo.cryptofetcher.model.CryptoPrice;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, CryptoPrice> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Configuraciones adicionales para reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, CryptoPrice> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

**Concepto clave - ACKS_CONFIG**:
- `acks=0`: No espera confirmación (rápido pero puede perder mensajes)
- `acks=1`: Espera confirmación del líder
- `acks=all`: Espera confirmación de todas las réplicas (más seguro)

### Paso 1.8: Crear KafkaTopicConfig

Crea `config/KafkaTopicConfig.java`:

```java
package com.espejo.cryptofetcher.config;

import com.espejo.cryptofetcher.utils.Constants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic cryptoPricesTopic() {
        return TopicBuilder.name(Constants.TOPIC_CRYPTO_PRICES)
                .partitions(3)  // Una partición por crypto
                .replicas(1)    // Solo 1 réplica para desarrollo
                .build();
    }
}
```

**Pregunta para ti**: ¿Por qué usamos 3 particiones?

<details>
<summary>Ver respuesta</summary>

Usamos 3 particiones porque tenemos 3 criptomonedas (BTC, ETH, SOL). Cada una irá a su propia partición basado en la key, lo que garantiza:
1. Orden de mensajes por criptomoneda
2. Paralelismo en el consumo
3. Distribución equitativa de carga

</details>

### Paso 1.9: Crear WebClientConfig

Crea `config/WebClientConfig.java`:

```java
package com.espejo.cryptofetcher.config;

import com.espejo.cryptofetcher.utils.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(Constants.COINGECKO_BASE_URL)
                .build();
    }
}
```

### Paso 1.10: Crear CoinGeckoService

Crea `service/CoinGeckoService.java`:

```java
package com.espejo.cryptofetcher.service;

import com.espejo.cryptofetcher.model.CryptoPrice;
import com.espejo.cryptofetcher.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoService {

    private final WebClient webClient;

    /**
     * Obtiene los precios actuales de las criptomonedas configuradas.
     *
     * API: GET /simple/price?ids=bitcoin,ethereum&vs_currencies=usd&include_market_cap=true&include_24hr_change=true
     */
    public Flux<CryptoPrice> fetchPrices() {
        String ids = String.join(",", Constants.CRYPTO_IDS);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/simple/price")
                        .queryParam("ids", ids)
                        .queryParam("vs_currencies", "usd")
                        .queryParam("include_market_cap", "true")
                        .queryParam("include_24hr_change", "true")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response -> log.debug("CoinGecko response: {}", response))
                .flatMapMany(this::mapToCryptoPrices)
                .doOnError(error -> log.error("Error fetching prices from CoinGecko", error));
    }

    /**
     * Convierte la respuesta de CoinGecko a objetos CryptoPrice.
     *
     * Respuesta ejemplo:
     * {
     *   "bitcoin": {
     *     "usd": 43000.50,
     *     "usd_market_cap": 840000000000,
     *     "usd_24h_change": 2.5
     *   }
     * }
     */
    private Flux<CryptoPrice> mapToCryptoPrices(Map<String, Map<String, Object>> response) {
        return Flux.fromIterable(response.entrySet())
                .map(entry -> {
                    String id = entry.getKey();
                    Map<String, Object> data = entry.getValue();

                    return CryptoPrice.builder()
                            .symbol(getSymbol(id))
                            .name(capitalize(id))
                            .priceUsd(toBigDecimal(data.get("usd")))
                            .priceChange24h(toBigDecimal(data.get("usd_24h_change")))
                            .marketCap(toBigDecimal(data.get("usd_market_cap")))
                            .timestamp(Instant.now())
                            .build();
                });
    }

    private String getSymbol(String id) {
        return switch (id) {
            case "bitcoin" -> Constants.BTC;
            case "ethereum" -> Constants.ETH;
            case "solana" -> Constants.SOL;
            default -> id.toUpperCase();
        };
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
```

### Paso 1.11: Crear PricePublisherService

Crea `service/PricePublisherService.java`:

```java
package com.espejo.cryptofetcher.service;

import com.espejo.cryptofetcher.model.CryptoPrice;
import com.espejo.cryptofetcher.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricePublisherService {

    private final KafkaTemplate<String, CryptoPrice> kafkaTemplate;

    /**
     * Publica un precio de criptomoneda al topic de Kafka.
     *
     * La KEY del mensaje es el símbolo (BTC, ETH, SOL).
     * Esto garantiza que todos los mensajes de una misma crypto
     * vayan a la misma partición, manteniendo el orden.
     */
    public void publishPrice(CryptoPrice price) {
        String key = price.getSymbol();

        CompletableFuture<SendResult<String, CryptoPrice>> future =
                kafkaTemplate.send(Constants.TOPIC_CRYPTO_PRICES, key, price);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published {} price: ${} to partition {}",
                        price.getSymbol(),
                        price.getPriceUsd(),
                        result.getRecordMetadata().partition());
            } else {
                log.error("Failed to publish {} price", price.getSymbol(), ex);
            }
        });
    }
}
```

**Concepto clave - Message Keys**:
- La KEY determina a qué partición va el mensaje
- Todos los mensajes con la misma KEY van a la misma partición
- Esto garantiza orden FIFO para mensajes con la misma KEY

### Paso 1.12: Crear PriceFetchScheduler

Crea `scheduler/PriceFetchScheduler.java`:

```java
package com.espejo.cryptofetcher.scheduler;

import com.espejo.cryptofetcher.service.CoinGeckoService;
import com.espejo.cryptofetcher.service.PricePublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceFetchScheduler {

    private final CoinGeckoService coinGeckoService;
    private final PricePublisherService pricePublisherService;

    /**
     * Ejecuta cada 5 minutos (300000 ms).
     * También ejecuta al iniciar la aplicación (initialDelay = 0).
     */
    @Scheduled(fixedRateString = "${scheduler.fetch-interval}", initialDelay = 0)
    public void fetchAndPublishPrices() {
        log.info("Starting scheduled price fetch...");

        coinGeckoService.fetchPrices()
                .doOnNext(price -> log.debug("Fetched: {} = ${}",
                        price.getSymbol(), price.getPriceUsd()))
                .subscribe(
                        pricePublisherService::publishPrice,
                        error -> log.error("Error in price fetch schedule", error),
                        () -> log.info("Completed price fetch cycle")
                );
    }
}
```

### Paso 1.13: Crear la Aplicación Principal

Crea `CryptoFetcherApplication.java`:

```java
package com.espejo.cryptofetcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Habilita @Scheduled
public class CryptoFetcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoFetcherApplication.class, args);
    }
}
```

### Paso 1.14: Crear Dockerfile

Crea `crypto-fetcher/Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Paso 1.15: Actualizar docker-compose.yml

Añade el nuevo servicio al `docker-compose.yml`:

```yaml
  crypto-fetcher:
    build:
      context: ./crypto-fetcher
      dockerfile: Dockerfile
    image: crypto-fetcher:latest
    container_name: crypto-fetcher
    depends_on:
      - kafka
    environment:
      KAFKA_SERVER: kafka:29092
    ports:
      - "8083:8083"
```

### Checkpoint Fase 1

**Prueba tu implementación:**

1. **Compilar el módulo**:
```bash
./gradlew :crypto-fetcher:build
```

2. **Iniciar infraestructura**:
```bash
docker-compose up -d zookeeper kafka redis
```

3. **Ejecutar el servicio**:
```bash
cd crypto-fetcher
../gradlew bootRun
```

4. **Verificar mensajes en Kafka** (en otra terminal):
```bash
docker exec -it <kafka-container-id> kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic crypto-prices \
  --from-beginning \
  --property print.key=true
```

**Resultado esperado**: Deberías ver mensajes como:
```
BTC	{"symbol":"BTC","name":"Bitcoin","priceUsd":43250.50,...}
ETH	{"symbol":"ETH","name":"Ethereum","priceUsd":2280.30,...}
SOL	{"symbol":"SOL","name":"Solana","priceUsd":98.45,...}
```

**Preguntas de autoevaluación**:
1. ¿Qué pasa si CoinGecko no responde?
2. ¿Cómo cambiarías el intervalo a 1 minuto?
3. ¿Por qué usamos `@Scheduled` en lugar de un while loop?

---

## Fase 2: Price Processor Service

### Objetivo de Aprendizaje
Aprenderás a crear un **Consumer** que procesa mensajes de Kafka y los almacena en Redis con estadísticas.

### Paso 2.1: Crear la Estructura del Módulo

```
price-processor/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── espejo/
│       │           └── priceprocessor/
│       │               ├── PriceProcessorApplication.java
│       │               ├── config/
│       │               │   ├── KafkaConsumerConfig.java
│       │               │   └── RedisConfig.java
│       │               ├── listener/
│       │               │   └── PriceListener.java
│       │               ├── service/
│       │               │   └── PriceStorageService.java
│       │               ├── repository/
│       │               │   ├── PriceRepository.java
│       │               │   └── PriceRepositoryImpl.java
│       │               ├── model/
│       │               │   ├── CryptoPrice.java
│       │               │   └── PriceStats.java
│       │               └── utils/
│       │                   └── Constants.java
│       └── resources/
│           └── application.yml
├── build.gradle
└── Dockerfile
```

### Paso 2.2: Configurar build.gradle

Crea `price-processor/build.gradle`:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.espejo'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'

    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### Paso 2.3: Actualizar settings.gradle

```gradle
rootProject.name = 'spring-kafka-redis-root'

include 'news-api'
include 'worker-service'
include 'crypto-fetcher'
include 'price-processor'  // NUEVO
```

### Paso 2.4: Crear los Modelos

Crea `model/CryptoPrice.java` (igual que en crypto-fetcher):

```java
package com.espejo.priceprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoPrice {
    private String symbol;
    private String name;
    private BigDecimal priceUsd;
    private BigDecimal priceChange24h;
    private BigDecimal marketCap;
    private Instant timestamp;
}
```

Crea `model/PriceStats.java`:

```java
package com.espejo.priceprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceStats {
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal avgPrice;
    private int sampleCount;
    private Instant lastUpdated;
}
```

### Paso 2.5: Crear Constants

```java
package com.espejo.priceprocessor.utils;

public class Constants {

    // Kafka
    public static final String TOPIC_CRYPTO_PRICES = "crypto-prices";
    public static final String CONSUMER_GROUP = "price-processor-group";

    // Redis keys
    public static final String REDIS_KEY_CURRENT = "crypto:current:";  // crypto:current:BTC
    public static final String REDIS_KEY_STATS = "crypto:stats:";      // crypto:stats:BTC
    public static final String REDIS_KEY_HISTORY = "crypto:history:";  // crypto:history:BTC

    private Constants() {}
}
```

### Paso 2.6: Configurar application.yml

```yaml
server:
  port: 8084

spring:
  application:
    name: price-processor

  kafka:
    bootstrap-servers: ${KAFKA_SERVER:localhost:29092}
    consumer:
      group-id: price-processor-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

  data:
    redis:
      host: ${REDIS_SERVER:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:myredis}

logging:
  level:
    com.espejo: DEBUG
```

### Paso 2.7: Crear KafkaConsumerConfig

```java
package com.espejo.priceprocessor.config;

import com.espejo.priceprocessor.model.CryptoPrice;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, CryptoPrice> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<CryptoPrice> deserializer = new JsonDeserializer<>(CryptoPrice.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CryptoPrice> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CryptoPrice> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // Un thread por partición
        return factory;
    }
}
```

**Concepto clave - Concurrency**:
- `setConcurrency(3)` crea 3 threads consumidores
- Cada thread procesa una partición
- Esto permite procesamiento paralelo por crypto

### Paso 2.8: Crear RedisConfig

```java
package com.espejo.priceprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);

        RedisSerializationContext<String, Object> context =
                RedisSerializationContext.<String, Object>newSerializationContext(keySerializer)
                        .value(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
```

### Paso 2.9: Crear PriceRepository

Interfaz `repository/PriceRepository.java`:

```java
package com.espejo.priceprocessor.repository;

import com.espejo.priceprocessor.model.CryptoPrice;
import com.espejo.priceprocessor.model.PriceStats;
import reactor.core.publisher.Mono;

public interface PriceRepository {

    Mono<Boolean> saveCurrentPrice(CryptoPrice price);

    Mono<CryptoPrice> getCurrentPrice(String symbol);

    Mono<Boolean> saveStats(PriceStats stats);

    Mono<PriceStats> getStats(String symbol);

    Mono<Long> addToHistory(String symbol, CryptoPrice price);
}
```

Implementación `repository/PriceRepositoryImpl.java`:

```java
package com.espejo.priceprocessor.repository;

import com.espejo.priceprocessor.model.CryptoPrice;
import com.espejo.priceprocessor.model.PriceStats;
import com.espejo.priceprocessor.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PriceRepositoryImpl implements PriceRepository {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Boolean> saveCurrentPrice(CryptoPrice price) {
        String key = Constants.REDIS_KEY_CURRENT + price.getSymbol();
        return redisTemplate.opsForValue()
                .set(key, price)
                .doOnSuccess(success -> log.debug("Saved current price for {}", price.getSymbol()));
    }

    @Override
    public Mono<CryptoPrice> getCurrentPrice(String symbol) {
        String key = Constants.REDIS_KEY_CURRENT + symbol;
        return redisTemplate.opsForValue()
                .get(key)
                .map(obj -> objectMapper.convertValue(obj, CryptoPrice.class));
    }

    @Override
    public Mono<Boolean> saveStats(PriceStats stats) {
        String key = Constants.REDIS_KEY_STATS + stats.getSymbol();
        return redisTemplate.opsForValue()
                .set(key, stats)
                .doOnSuccess(success -> log.debug("Saved stats for {}", stats.getSymbol()));
    }

    @Override
    public Mono<PriceStats> getStats(String symbol) {
        String key = Constants.REDIS_KEY_STATS + symbol;
        return redisTemplate.opsForValue()
                .get(key)
                .map(obj -> objectMapper.convertValue(obj, PriceStats.class));
    }

    @Override
    public Mono<Long> addToHistory(String symbol, CryptoPrice price) {
        String key = Constants.REDIS_KEY_HISTORY + symbol;
        return redisTemplate.opsForList()
                .rightPush(key, price)
                .doOnSuccess(size -> log.debug("Added to history for {}, size: {}", symbol, size));
    }
}
```

### Paso 2.10: Crear PriceStorageService

```java
package com.espejo.priceprocessor.service;

import com.espejo.priceprocessor.model.CryptoPrice;
import com.espejo.priceprocessor.model.PriceStats;
import com.espejo.priceprocessor.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceStorageService {

    private final PriceRepository priceRepository;

    /**
     * Procesa y almacena un nuevo precio.
     * 1. Guarda el precio actual
     * 2. Añade al historial
     * 3. Actualiza estadísticas
     */
    public Mono<Void> processPrice(CryptoPrice price) {
        return priceRepository.saveCurrentPrice(price)
                .then(priceRepository.addToHistory(price.getSymbol(), price))
                .then(updateStats(price))
                .then()
                .doOnSuccess(v -> log.info("Processed price for {}: ${}",
                        price.getSymbol(), price.getPriceUsd()));
    }

    /**
     * Actualiza las estadísticas (min, max, avg) para una criptomoneda.
     */
    private Mono<Boolean> updateStats(CryptoPrice price) {
        return priceRepository.getStats(price.getSymbol())
                .defaultIfEmpty(createInitialStats(price))
                .map(stats -> updateStatsWithNewPrice(stats, price))
                .flatMap(priceRepository::saveStats);
    }

    private PriceStats createInitialStats(CryptoPrice price) {
        return PriceStats.builder()
                .symbol(price.getSymbol())
                .currentPrice(price.getPriceUsd())
                .minPrice(price.getPriceUsd())
                .maxPrice(price.getPriceUsd())
                .avgPrice(price.getPriceUsd())
                .sampleCount(0)
                .lastUpdated(Instant.now())
                .build();
    }

    private PriceStats updateStatsWithNewPrice(PriceStats stats, CryptoPrice price) {
        BigDecimal newPrice = price.getPriceUsd();
        int newCount = stats.getSampleCount() + 1;

        // Calcular nuevo promedio: ((oldAvg * oldCount) + newPrice) / newCount
        BigDecimal newAvg = stats.getAvgPrice()
                .multiply(BigDecimal.valueOf(stats.getSampleCount()))
                .add(newPrice)
                .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        return PriceStats.builder()
                .symbol(stats.getSymbol())
                .currentPrice(newPrice)
                .minPrice(stats.getMinPrice().min(newPrice))
                .maxPrice(stats.getMaxPrice().max(newPrice))
                .avgPrice(newAvg)
                .sampleCount(newCount)
                .lastUpdated(Instant.now())
                .build();
    }
}
```

### Paso 2.11: Crear PriceListener

```java
package com.espejo.priceprocessor.listener;

import com.espejo.priceprocessor.model.CryptoPrice;
import com.espejo.priceprocessor.service.PriceStorageService;
import com.espejo.priceprocessor.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceListener {

    private final PriceStorageService priceStorageService;

    @KafkaListener(
            topics = Constants.TOPIC_CRYPTO_PRICES,
            groupId = Constants.CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPriceReceived(ConsumerRecord<String, CryptoPrice> record) {
        String key = record.key();
        CryptoPrice price = record.value();
        int partition = record.partition();
        long offset = record.offset();

        log.info("Received {} from partition {} at offset {}", key, partition, offset);

        priceStorageService.processPrice(price)
                .subscribe(
                        null,
                        error -> log.error("Error processing price for {}", key, error)
                );
    }
}
```

**Concepto clave - ConsumerRecord**:
- Contiene metadata del mensaje: partition, offset, key, value, timestamp
- El offset es la posición del mensaje en la partición
- Kafka trackea qué offsets ha leído cada consumer group

### Paso 2.12: Crear la Aplicación Principal

```java
package com.espejo.priceprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PriceProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceProcessorApplication.class, args);
    }
}
```

### Paso 2.13: Actualizar docker-compose.yml

```yaml
  price-processor:
    build:
      context: ./price-processor
      dockerfile: Dockerfile
    image: price-processor:latest
    container_name: price-processor
    depends_on:
      - kafka
      - redis
    environment:
      KAFKA_SERVER: kafka:29092
      REDIS_SERVER: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: myredis
    ports:
      - "8084:8084"
```

### Checkpoint Fase 2

**Prueba tu implementación:**

1. **Compilar**:
```bash
./gradlew :price-processor:build
```

2. **Ejecutar ambos servicios**:
```bash
# Terminal 1
cd crypto-fetcher && ../gradlew bootRun

# Terminal 2
cd price-processor && ../gradlew bootRun
```

3. **Verificar datos en Redis**:
```bash
docker exec -it <redis-container-id> redis-cli -a myredis

# Comandos Redis
KEYS crypto:*
GET crypto:current:BTC
GET crypto:stats:BTC
LRANGE crypto:history:BTC 0 -1
```

**Resultado esperado**:
- Precios actuales guardados
- Estadísticas calculadas con min, max, avg
- Historial creciendo con cada fetch

---

## Fase 3: Alert Service

### Objetivo de Aprendizaje
Aprenderás a crear un servicio que consume del mismo topic (consumer group diferente), detecta condiciones y publica a un nuevo topic.

### Paso 3.1: Crear la Estructura

```
alert-service/
├── src/main/java/com/espejo/alertservice/
│   ├── AlertServiceApplication.java
│   ├── config/
│   │   ├── KafkaConfig.java
│   │   └── KafkaTopicConfig.java
│   ├── listener/
│   │   └── PriceAlertListener.java
│   ├── service/
│   │   ├── AlertDetectionService.java
│   │   └── AlertPublisherService.java
│   ├── model/
│   │   ├── CryptoPrice.java
│   │   └── PriceAlert.java
│   └── utils/
│       └── Constants.java
├── build.gradle
└── Dockerfile
```

### Paso 3.2: Crear PriceAlert Model

```java
package com.espejo.alertservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlert {
    private String symbol;
    private String alertType;        // PRICE_INCREASE, PRICE_DECREASE
    private BigDecimal previousPrice;
    private BigDecimal currentPrice;
    private BigDecimal changePercent;
    private Instant timestamp;
    private String message;
}
```

### Paso 3.3: Crear Constants

```java
package com.espejo.alertservice.utils;

public class Constants {

    // Kafka Topics
    public static final String TOPIC_CRYPTO_PRICES = "crypto-prices";
    public static final String TOPIC_PRICE_ALERTS = "price-alerts";

    // Consumer Group (diferente al price-processor!)
    public static final String CONSUMER_GROUP = "alert-service-group";

    // Alert thresholds
    public static final double ALERT_THRESHOLD_PERCENT = 5.0;

    // Alert types
    public static final String ALERT_PRICE_INCREASE = "PRICE_INCREASE";
    public static final String ALERT_PRICE_DECREASE = "PRICE_DECREASE";

    private Constants() {}
}
```

**Concepto clave - Consumer Groups**:
- `price-processor-group` y `alert-service-group` son grupos diferentes
- Ambos reciben TODOS los mensajes del topic
- Si fueran el mismo grupo, se repartirían los mensajes

### Paso 3.4: Crear AlertDetectionService

```java
package com.espejo.alertservice.service;

import com.espejo.alertservice.model.CryptoPrice;
import com.espejo.alertservice.model.PriceAlert;
import com.espejo.alertservice.utils.Constants;s
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AlertDetectionService {

    // Cache de último precio por símbolo
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    /**
     * Evalúa si el nuevo precio genera una alerta.
     * Retorna Optional.empty() si no hay alerta.
     */
    public Optional<PriceAlert> evaluatePrice(CryptoPrice price) {
        String symbol = price.getSymbol();
        BigDecimal currentPrice = price.getPriceUsd();
        BigDecimal previousPrice = lastPrices.get(symbol);

        // Actualizar cache
        lastPrices.put(symbol, currentPrice);

        // Si es el primer precio, no hay alerta
        if (previousPrice == null) {
            log.debug("First price for {}, no alert", symbol);
            return Optional.empty();
        }

        // Calcular cambio porcentual
        BigDecimal change = currentPrice.subtract(previousPrice)
                .divide(previousPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        double changePercent = change.doubleValue();

        // Verificar si excede el umbral
        if (Math.abs(changePercent) >= Constants.ALERT_THRESHOLD_PERCENT) {
            String alertType = changePercent > 0
                    ? Constants.ALERT_PRICE_INCREASE
                    : Constants.ALERT_PRICE_DECREASE;

            PriceAlert alert = PriceAlert.builder()
                    .symbol(symbol)
                    .alertType(alertType)
                    .previousPrice(previousPrice)
                    .currentPrice(currentPrice)
                    .changePercent(change.setScale(2, RoundingMode.HALF_UP))
                    .timestamp(Instant.now())
                    .message(String.format("%s %s %.2f%% from $%s to $%s",
                            symbol,
                            changePercent > 0 ? "increased" : "decreased",
                            Math.abs(changePercent),
                            previousPrice,
                            currentPrice))
                    .build();

            log.warn("ALERT: {}", alert.getMessage());
            return Optional.of(alert);
        }

        log.debug("{} change: {}% (threshold: {}%)",
                symbol, change, Constants.ALERT_THRESHOLD_PERCENT);
        return Optional.empty();
    }
}
```

### Paso 3.5: Crear AlertPublisherService

```java
package com.espejo.alertservice.service;

import com.espejo.alertservice.model.PriceAlert;
import com.espejo.alertservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertPublisherService {

    private final KafkaTemplate<String, PriceAlert> kafkaTemplate;

    public void publishAlert(PriceAlert alert) {
        kafkaTemplate.send(Constants.TOPIC_PRICE_ALERTS, alert.getSymbol(), alert)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published alert: {}", alert.getMessage());
                    } else {
                        log.error("Failed to publish alert", ex);
                    }
                });
    }
}
```

### Paso 3.6: Crear PriceAlertListener

```java
package com.espejo.alertservice.listener;

import com.espejo.alertservice.model.CryptoPrice;
import com.espejo.alertservice.service.AlertDetectionService;
import com.espejo.alertservice.service.AlertPublisherService;
import com.espejo.alertservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAlertListener {

    private final AlertDetectionService alertDetectionService;
    private final AlertPublisherService alertPublisherService;

    @KafkaListener(
            topics = Constants.TOPIC_CRYPTO_PRICES,
            groupId = Constants.CONSUMER_GROUP
    )
    public void onPriceReceived(CryptoPrice price) {
        log.debug("Evaluating {} for alerts...", price.getSymbol());

        alertDetectionService.evaluatePrice(price)
                .ifPresent(alertPublisherService::publishAlert);
    }
}
```

### Checkpoint Fase 3

**Prueba tu implementación:**

1. Ejecutar los 3 servicios
2. Para simular una alerta, puedes:
   - Modificar temporalmente el umbral a 0.1%
   - O esperar volatilidad real del mercado

3. Verificar alertas:
```bash
docker exec -it <kafka-container-id> kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-alerts \
  --from-beginning
```

---

## Fase 4: Crypto API

### Objetivo de Aprendizaje
Aprenderás a exponer los datos procesados via REST API, similar a tu `news-api` existente.

### Endpoints a Implementar

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/crypto/prices` | Lista precios actuales de todas las cryptos |
| GET | `/api/v1/crypto/prices/{symbol}` | Precio actual de una crypto |
| GET | `/api/v1/crypto/stats/{symbol}` | Estadísticas de una crypto |
| GET | `/api/v1/crypto/alerts` | Últimas alertas (desde Redis) |

### Estructura Sugerida

```
crypto-api/
├── src/main/java/com/espejo/cryptoapi/
│   ├── CryptoApiApplication.java
│   ├── config/
│   │   └── RedisConfig.java
│   ├── controller/
│   │   └── CryptoController.java
│   ├── service/
│   │   └── CryptoService.java
│   ├── repository/
│   │   └── CryptoRepository.java
│   └── model/
│       ├── CryptoPrice.java
│       └── PriceStats.java
```

### Implementación Base del Controller

```java
@RestController
@RequestMapping("/api/v1/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoService cryptoService;

    @GetMapping("/prices")
    public Mono<ResponseEntity<List<CryptoPrice>>> getAllPrices() {
        return cryptoService.getAllCurrentPrices()
                .collectList()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/prices/{symbol}")
    public Mono<ResponseEntity<CryptoPrice>> getPrice(@PathVariable String symbol) {
        return cryptoService.getCurrentPrice(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats/{symbol}")
    public Mono<ResponseEntity<PriceStats>> getStats(@PathVariable String symbol) {
        return cryptoService.getStats(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
```

---

## Ejercicios Adicionales

Una vez que completes las 4 fases, aquí hay ejercicios para profundizar:

### Ejercicio 1: Kafka Streams
Reemplaza `alert-service` con Kafka Streams para procesamiento real-time:
```java
KStream<String, CryptoPrice> prices = builder.stream("crypto-prices");
prices.filter((key, value) -> detectAlert(value))
      .to("price-alerts");
```

### Ejercicio 2: Dead Letter Queue
Implementa un DLQ para mensajes que fallan al procesar:
- Topic: `crypto-prices-dlq`
- Reintentos automáticos
- Logging de errores

### Ejercicio 3: Subscripciones de Usuario
Permite a usuarios suscribirse a alertas de cryptos específicas:
- Endpoint: POST `/api/v1/subscriptions`
- Topic: `user-subscriptions`
- Filtrado por usuario

### Ejercicio 4: Websocket
Expón los precios en tiempo real via WebSocket:
- Endpoint: `/ws/prices`
- Actualización cada vez que llega un nuevo precio

### Ejercicio 5: Múltiples Particiones
Escala el sistema:
- 10 particiones para `crypto-prices`
- 3 instancias de `price-processor`
- Verifica distribución de carga

---

## Recursos Adicionales

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [CoinGecko API Docs](https://www.coingecko.com/en/api/documentation)
- [Kafka Streams](https://kafka.apache.org/documentation/streams/)

---

## Troubleshooting

### Error: "Topic not found"
```bash
# Verificar topics existentes
docker exec -it <kafka-container> kafka-topics --list --bootstrap-server localhost:9092
```

### Error: "Connection refused"
- Verifica que Kafka esté running
- Verifica el bootstrap server en application.yml

### Error: "Deserialization error"
- Verifica que el modelo sea idéntico en producer y consumer
- Verifica `trusted.packages` en consumer config

### Mensajes no se consumen
- Verifica el `group-id`
- Verifica `auto-offset-reset`
- Revisa logs del consumer

---

**Próximo paso**: Empieza por la Fase 1. Cuando tengas dudas o completes un checkpoint, puedo ayudarte a revisar y continuar.