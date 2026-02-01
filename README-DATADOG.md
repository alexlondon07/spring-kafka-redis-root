# Observabilidad con Datadog

Este proyecto incluye integración completa con Datadog para monitoreo, trazas APM, métricas y logs.

## Configuración Inicial

### 1. Obtener API Key de Datadog

1. Crea una cuenta en [Datadog](https://www.datadoghq.com/) si no tienes una
2. Ve a [Organization Settings > API Keys](https://app.datadoghq.com/organization-settings/api-keys)
3. Copia tu API key

### 2. Configurar Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto:

```bash
cp .env.example .env
```

Edita el archivo `.env` y agrega tu API key:

```env
DD_API_KEY=tu_api_key_aqui
DD_SITE=datadoghq.com  # o datadoghq.eu si estás en Europa
```

### 3. Iniciar el Stack Completo

```bash
# Construir todos los servicios
./gradlew clean build

# Iniciar todos los servicios con Docker Compose
docker-compose up --build -d
```

## Características de Observabilidad

### 📊 Métricas (Metrics)

Todos los microservicios exportan métricas automáticamente a Datadog cada 10 segundos:

- **Métricas JVM**: Heap memory, GC, threads, CPU
- **Métricas de negocio**: Custom counters, gauges, timers
- **Métricas de Spring Boot**: HTTP requests, Kafka consumers/producers
- **Métricas de Redis**: Conexiones, latencia
- **Métricas de Kafka**: Consumer lag, throughput, errores

**Visualizar métricas:**
1. Ve a [Datadog Metrics Explorer](https://app.datadoghq.com/metric/explorer)
2. Busca métricas por servicio:
   - `service:news-api`
   - `service:worker-service`
   - `service:crypto-fetcher-service`
   - `service:price-processor-service`
   - `service:alert-service`

### 🔍 Trazas APM (Application Performance Monitoring)

El agente Datadog Java captura automáticamente trazas distribuidas:

- **Requests HTTP** en endpoints REST
- **Mensajes Kafka** (producer y consumer)
- **Queries a Redis**
- **Llamadas HTTP externas** (MediaStack API, CoinCap API)
- **Trace correlation** entre servicios

**Visualizar trazas:**
1. Ve a [Datadog APM > Traces](https://app.datadoghq.com/apm/traces)
2. Filtra por servicio o endpoint
3. Analiza latencias y errores

### 📝 Logs

Los logs de todos los contenedores se envían automáticamente a Datadog:

- Logs de aplicación (Spring Boot)
- Logs de infraestructura (Kafka, Redis)
- Correlación automática con trazas (trace_id, span_id)

**Visualizar logs:**
1. Ve a [Datadog Logs](https://app.datadoghq.com/logs)
2. Filtra por servicio:
   ```
   service:news-api
   ```
3. Filtra por nivel:
   ```
   status:error
   ```

### 📈 Dashboards Recomendados

#### Dashboard de Servicios Java
Crea un dashboard con estos widgets:

1. **JVM Heap Memory**
   ```
   avg:jvm.memory.used{service:news-api} by {service}
   ```

2. **Request Rate**
   ```
   sum:http.server.requests{service:news-api}.as_count()
   ```

3. **Error Rate**
   ```
   sum:http.server.requests{service:news-api,status:5xx}.as_count()
   ```

4. **Response Time (p95)**
   ```
   avg:http.server.requests{service:news-api}.p95()
   ```

#### Dashboard de Kafka
1. **Consumer Lag**
   ```
   avg:kafka.consumer.lag{*} by {service}
   ```

2. **Messages Consumed**
   ```
   sum:kafka.consumer.records.consumed{*} by {service}
   ```

3. **Processing Errors**
   ```
   sum:kafka.consumer.errors{*} by {service}
   ```

### 🔔 Alertas Recomendadas

#### Alerta de Error Rate Alto
```yaml
Condición: Error rate > 5% durante 5 minutos
Métrica: http.server.requests{status:5xx}
Notificación: Email, Slack
```

#### Alerta de Consumer Lag Alto
```yaml
Condición: Consumer lag > 1000 mensajes durante 10 minutos
Métrica: kafka.consumer.lag
Notificación: Email, PagerDuty
```

#### Alerta de Memory Alto
```yaml
Condición: JVM Heap > 90% durante 5 minutos
Métrica: jvm.memory.used / jvm.memory.max
Notificación: Email
```

## Endpoints de Actuator

Cada servicio expone endpoints de Actuator:

- **Health**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`
- **Prometheus**: `http://localhost:8080/actuator/prometheus`

Ejemplo:
```bash
# Health check de news-api
curl http://localhost:8080/actuator/health

# Métricas de JVM
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## Servicios y Tags

Cada servicio tiene los siguientes tags en Datadog:

| Servicio | Puerto | Tags |
|----------|--------|------|
| news-api | 8080 | `service:news-api`, `env:docker-local`, `version:1.0.0` |
| worker-service | 8081 | `service:worker-service`, `env:docker-local`, `version:1.0.0` |
| crypto-fetcher-service | 8083 | `service:crypto-fetcher-service`, `env:docker-local`, `version:1.0.0` |
| price-processor-service | 8084 | `service:price-processor-service`, `env:docker-local`, `version:1.0.0` |
| alert-service | 8085 | `service:alert-service`, `env:docker-local`, `version:1.0.0` |

## Troubleshooting

### No veo métricas en Datadog
1. Verifica que el archivo `.env` tiene la API key correcta
2. Verifica que el contenedor `datadog-agent` está corriendo:
   ```bash
   docker-compose logs datadog-agent
   ```
3. Verifica la conectividad:
   ```bash
   docker-compose exec datadog-agent agent status
   ```

### No veo trazas APM
1. Verifica que los servicios tienen el agente Java:
   ```bash
   docker-compose logs news-api | grep "dd-java-agent"
   ```
2. Verifica que `DD_TRACE_ENABLED=true` en docker-compose.yml
3. Espera 1-2 minutos para que las trazas aparezcan

### Logs no aparecen
1. Verifica que `DD_LOGS_ENABLED=true` en el agente Datadog
2. Verifica los labels en docker-compose.yml:
   ```yaml
   labels:
     com.datadoghq.ad.logs: '[{"source": "java", "service": "news-api"}]'
   ```

## Referencias

- [Datadog Java APM](https://docs.datadoghq.com/tracing/setup_overview/setup/java/)
- [Micrometer Datadog Registry](https://micrometer.io/docs/registry/datadog)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Datadog Agent Docker](https://docs.datadoghq.com/agent/docker/)
