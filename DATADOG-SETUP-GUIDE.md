# Guía Completa de Configuración de Datadog

## 📊 Dashboards Disponibles

He creado 3 dashboards principales en formato JSON que puedes importar directamente en Datadog:

### 1. **Microservices Overview - JVM Metrics**

Dashboard enfocado en métricas de la JVM para todos los microservicios.

**Widgets incluidos:**
- JVM Heap Memory Usage by Service (línea temporal)
- JVM Heap Memory % by Service (valor actual)
- GC Pause Time P95 (barras)
- GC Collections per Minute (línea)
- Thread Count by Service (área)
- Classes Loaded (línea)
- CPU Usage by Service (top list)
- Non-Heap Memory - Metaspace, Code Cache (área)

**Métricas clave:**
```
jvm.memory.used{memory:heap}
jvm.memory.max{memory:heap}
jvm.gc.pause
jvm.threads.live
jvm.classes.loaded
process.runtime.jvm.cpu.utilization
```

### 2. **Kafka Metrics Dashboard**

Dashboard especializado en métricas de Kafka (producers y consumers).

**Widgets incluidos:**
- Kafka Consumer Lag by Service (línea con threshold)
- Messages Consumed Rate (barras)
- Messages Produced Rate (barras)
- Consumer Processing Time P95 (línea)
- Consumer Errors (barras)
- Producer Request Rate (valor)
- Consumer Lag Heatmap
- Consumer Groups Status (check status)

**Métricas clave:**
```
kafka.consumer.lag
kafka.consumer.records.consumed
kafka.producer.record.send.total
spring.kafka.listener.seconds
kafka.consumer.errors
```

### 3. **Business Metrics - Crypto & News**

Dashboard de métricas de negocio específicas del sistema.

**Widgets incluidos:**
- HTTP Requests per Minute by Service (barras)
- HTTP Request Latency P95 (línea)
- HTTP Error Rate 4xx + 5xx (barras)
- Crypto Prices Fetched (valor)
- Price Alerts Triggered (valor)
- News Requests Processed (barras)
- Redis Cache Hit Rate (valor %)
- Redis Connections (línea)
- Top 10 Slowest Endpoints (top list)
- Request Success Rate (valor con colores)

**Métricas clave:**
```
http.server.requests
kafka.producer.record.send.total{topic:crypto-prices}
kafka.producer.record.send.total{topic:price-alerts}
redis.keyspace.hits
redis.keyspace.misses
redis.net.clients
```

## 🚨 Alertas Configuradas

He creado 8 monitores críticos en `datadog-alerts.json`:

### Alerta 1: High Error Rate
- **Trigger**: Error rate > 10 errors/sec (5 errors/sec warning)
- **Window**: Last 5 minutes
- **Notifica**: Slack, PagerDuty
- **Prioridad**: High

### Alerta 2: Consumer Lag Too High ⚠️
- **Trigger**: Lag > 1000 messages (500 warning)
- **Window**: Last 10 minutes
- **Notifica**: Slack Kafka channel, On-call
- **Prioridad**: Critical

### Alerta 3: Memory Usage Above 90% 🔴
- **Trigger**: Heap > 90% (80% warning)
- **Window**: Last 15 minutes
- **Notifica**: Slack, On-call, PagerDuty
- **Prioridad**: Critical

### Alerta 4: Slow API Response Time
- **Trigger**: P95 latency > 2 seconds (1 second warning)
- **Window**: Last 10 minutes
- **Notifica**: Slack performance channel
- **Prioridad**: Medium

### Alerta 5: Service Down 🔴
- **Trigger**: Health check failing
- **Check**: Last 2 checks
- **Notifica**: Slack critical, PagerDuty, On-call
- **Prioridad**: Critical

### Alerta 6: High Redis Connection Count
- **Trigger**: Connections > 100 (75 warning)
- **Window**: Last 10 minutes
- **Prioridad**: Medium

### Alerta 7: Excessive Garbage Collection
- **Trigger**: GC rate > 10/sec (5/sec warning)
- **Window**: Last 15 minutes
- **Prioridad**: Medium

### Alerta 8: No Crypto Prices Fetched
- **Trigger**: No messages in last 30 minutes
- **Window**: Last 30 minutes
- **Prioridad**: High

## 🗺️ Service Map - Arquitectura del Sistema

### **Cómo Acceder al Service Map**

1. Ve a: https://app.datadoghq.com/apm/map
2. Filtra por `env:docker-local`
3. Selecciona el tiempo: Last 1 hour

### **Arquitectura Visualizada**

```
┌─────────────────────────────────────────────────────────────────┐
│                        SERVICE MAP                              │
└─────────────────────────────────────────────────────────────────┘

                          ┌──────────────┐
                          │   Client     │
                          │  (Browser)   │
                          └──────┬───────┘
                                 │ HTTP GET
                                 │
                    ┌────────────▼─────────────┐
                    │      news-api            │
                    │      Port: 8080          │
                    │  ┌──────────────────┐    │
                    │  │  Endpoints:      │    │
                    │  │  /api/v1/news    │    │
                    │  └──────────────────┘    │
                    └────┬──────────────┬──────┘
                         │              │
              Kafka Pub  │              │ Redis Cache
              Topic:news │              │ Read/Write
                         │              │
                         │         ┌────▼────────┐
                         │         │   Redis     │
                         │         │   Port:6379 │
                         │         └─────────────┘
                         │
              ┌──────────▼──────────┐
              │   Apache Kafka      │
              │   Port: 29092       │
              │  ┌──────────────┐   │
              │  │ Topics:      │   │
              │  │ - news       │   │
              │  │ - crypto-    │   │
              │  │   prices     │   │
              │  │ - price-     │   │
              │  │   alerts     │   │
              │  └──────────────┘   │
              └──┬─────────┬────┬───┘
                 │         │    │
    Kafka Sub   │         │    │
                 │         │    │
      ┌──────────▼──┐  ┌──▼────▼──────────┐  ┌────────────────┐
      │ worker-     │  │ crypto-fetcher-  │  │ price-         │
      │ service     │  │ service          │  │ processor-     │
      │ Port: 8081  │  │ Port: 8083       │  │ service        │
      │             │  │                  │  │ Port: 8084     │
      │ Consumes:   │  │ Produces:        │  │                │
      │ - news      │  │ - crypto-prices  │  │ Consumes:      │
      │             │  │                  │  │ - crypto-prices│
      │ Publishes:  │  │ External API:    │  │                │
      │ Redis cache │  │ CoinCap API      │  │ Produces:      │
      │             │  │                  │  │ Redis cache    │
      │ Calls:      │  └──────────────────┘  └────────┬───────┘
      │ MediaStack  │                                  │
      │ API         │                                  │
      └─────────────┘                    Kafka Pub     │
                                        Topic: crypto-prices
                                                       │
                                          ┌────────────▼───────┐
                                          │   alert-service    │
                                          │   Port: 8085       │
                                          │                    │
                                          │   Consumes:        │
                                          │   - crypto-prices  │
                                          │                    │
                                          │   Detects >5%      │
                                          │   price changes    │
                                          │                    │
                                          │   Produces:        │
                                          │   - price-alerts   │
                                          └────────────────────┘
```

### **Flujos Principales**

#### Flow 1: News Request
```
Client → news-api → Redis (check cache)
                 ├─ Cache Hit → Return data (200)
                 └─ Cache Miss → Publish to Kafka topic "news" (404)
                              → worker-service consumes message
                              → Fetch from MediaStack API
                              → Store in Redis
```

#### Flow 2: Crypto Price Processing
```
crypto-fetcher-service (scheduled) → Fetch from CoinCap API
                                  → Publish to Kafka "crypto-prices"
                                  → price-processor-service consumes
                                  → Store in Redis
                                  → alert-service consumes
                                  → Detect price changes >5%
                                  → Publish to Kafka "price-alerts"
```

### **Métricas Clave por Flujo**

**News Flow:**
- `http.server.requests{service:news-api,uri:/api/v1/news}`
- `kafka.producer.record.send.total{topic:news}`
- `kafka.consumer.lag{service:worker-service,topic:news}`
- `redis.keyspace.hits` vs `redis.keyspace.misses`

**Crypto Price Flow:**
- `kafka.producer.record.send.total{service:crypto-fetcher-service,topic:crypto-prices}`
- `kafka.consumer.lag{service:price-processor-service,topic:crypto-prices}`
- `kafka.consumer.lag{service:alert-service,topic:crypto-prices}`
- `kafka.producer.record.send.total{service:alert-service,topic:price-alerts}`

## 📥 Importar Dashboards y Alertas

### Opción 1: Via UI (Recomendado)

**Dashboards:**
1. Ve a: https://app.datadoghq.com/dashboard/lists
2. Click "New Dashboard" → "Import Dashboard"
3. Copia el contenido de `datadog-dashboards.json`
4. Pega en el editor
5. Click "Save"
6. Repite para cada dashboard (JVM, Kafka, Business)

**Alertas:**
1. Ve a: https://app.datadoghq.com/monitors/manage
2. Click "New Monitor"
3. Selecciona el tipo según el alert
4. Copia la configuración de `datadog-alerts.json`
5. Ajusta notification channels (@slack, @pagerduty)
6. Click "Create"
7. Repite para cada monitor

### Opción 2: Via API

```bash
# Importar dashboard
curl -X POST "https://api.datadoghq.com/api/v1/dashboard" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d @datadog-dashboards.json

# Importar monitor/alert
curl -X POST "https://api.datadoghq.com/api/v1/monitor" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d @datadog-alerts.json
```

### Opción 3: Via Terraform (Infraestructura como Código)

```hcl
# datadog_monitors.tf
resource "datadog_monitor" "high_error_rate" {
  name    = "[Microservices] High Error Rate"
  type    = "metric alert"
  message = "..."
  query   = "sum(last_5m):sum:http.server.requests{status:5*}.as_rate() by {service} > 10"

  monitor_thresholds {
    critical = 10
    warning  = 5
  }

  tags = ["env:docker-local", "team:backend"]
}
```

## 🔔 Configurar Notificaciones

### Slack Integration

1. Ve a: https://app.datadoghq.com/account/settings#integrations/slack
2. Click "Add Slack Account"
3. Autoriza Datadog en tu workspace
4. Configura channels:
   - `#alerts` → @slack-alerts
   - `#kafka-alerts` → @slack-kafka
   - `#performance` → @slack-performance
   - `#critical` → @slack-critical

### PagerDuty Integration

1. Ve a: https://app.datadoghq.com/account/settings#integrations/pagerduty
2. Añade tu PagerDuty API key
3. Configura escalation policies
4. Mapea servicios a on-call schedules

## 📊 Métricas Disponibles en APM

El Datadog Java Agent (dd-java-agent.jar) automáticamente instrumenta:

### HTTP Requests
- Request rate, latency, errors
- Endpoint breakdown
- Status code distribution

### Kafka
- Producer/Consumer metrics
- Lag monitoring
- Throughput

### Database (Redis)
- Query performance
- Connection pooling
- Cache hit rate

### JVM
- Heap/Non-heap memory
- GC metrics
- Thread pools
- Class loading

## 🎯 KPIs Recomendados

### Availability
- **Service Uptime**: > 99.9%
- **Health Check Success Rate**: > 99.5%

### Performance
- **API Response Time (P95)**: < 500ms
- **Kafka Consumer Lag**: < 100 messages
- **Redis Cache Hit Rate**: > 80%

### Reliability
- **Error Rate**: < 1%
- **Success Rate**: > 99%
- **GC Pause Time**: < 100ms

### Resource Usage
- **JVM Heap Usage**: < 80%
- **CPU Usage**: < 70%
- **Thread Count**: Stable

## 🚀 Próximos Pasos

1. ✅ Importar dashboards
2. ✅ Configurar alertas
3. ✅ Setup Slack notifications
4. 🔄 Configurar PagerDuty (opcional)
5. 🔄 Crear custom metrics para business logic
6. 🔄 Setup SLOs (Service Level Objectives)
7. 🔄 Configurar Synthetic tests para endpoints críticos
8. 🔄 Habilitar Continuous Profiler

## 📚 Recursos Adicionales

- [Datadog APM Documentation](https://docs.datadoghq.com/tracing/)
- [Java APM Best Practices](https://docs.datadoghq.com/tracing/setup_overview/setup/java/)
- [Dashboard Best Practices](https://docs.datadoghq.com/dashboards/guide/best-practices/)
- [Monitor Best Practices](https://docs.datadoghq.com/monitors/guide/best-practices/)
