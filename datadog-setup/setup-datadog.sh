#!/bin/bash

echo "🚀 Setting up Datadog Dashboards and Monitors..."

# Wait for Datadog agent to be ready
echo "⏳ Waiting for Datadog agent to be ready..."
sleep 10

# Check if API keys are set
if [ -z "$DD_API_KEY" ]; then
    echo "❌ Error: DD_API_KEY is not set"
    exit 1
fi

if [ -z "$DD_APP_KEY" ]; then
    echo "❌ Error: DD_APP_KEY is not set"
    echo "ℹ️  Please generate an Application Key at: https://app.datadoghq.com/organization-settings/application-keys"
    exit 1
fi

DD_SITE="${DD_SITE:-datadoghq.com}"
API_BASE="https://api.${DD_SITE}/api/v1"

echo "📊 Creating Dashboards..."

# Dashboard 1: JVM Metrics
echo "  Creating JVM Metrics Dashboard..."
curl -X POST "${API_BASE}/dashboard" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d '{
    "title": "Microservices Overview - JVM Metrics",
    "description": "Dashboard con métricas JVM de todos los microservicios",
    "widgets": [
      {
        "definition": {
          "title": "JVM Heap Memory Usage by Service",
          "type": "timeseries",
          "requests": [
            {
              "q": "avg:jvm.memory.used{memory:heap,env:docker-local} by {service}",
              "display_type": "line",
              "style": {
                "palette": "dog_classic",
                "line_type": "solid",
                "line_width": "normal"
              }
            },
            {
              "q": "avg:jvm.memory.max{memory:heap,env:docker-local} by {service}",
              "display_type": "line",
              "style": {
                "palette": "warm",
                "line_type": "dashed",
                "line_width": "normal"
              }
            }
          ],
          "yaxis": {
            "label": "",
            "scale": "linear",
            "min": "auto",
            "max": "auto",
            "include_zero": true
          }
        },
        "layout": {
          "x": 0,
          "y": 0,
          "width": 6,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "JVM Heap Memory %",
          "type": "query_value",
          "requests": [
            {
              "q": "(avg:jvm.memory.used{memory:heap,env:docker-local} by {service} / avg:jvm.memory.max{memory:heap,env:docker-local} by {service}) * 100",
              "aggregator": "last"
            }
          ],
          "precision": 2,
          "autoscale": true
        },
        "layout": {
          "x": 6,
          "y": 0,
          "width": 2,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "GC Pause Time (P95)",
          "type": "timeseries",
          "requests": [
            {
              "q": "avg:jvm.gc.pause{env:docker-local} by {service,action}.rollup(avg, 60)",
              "display_type": "bars"
            }
          ]
        },
        "layout": {
          "x": 8,
          "y": 0,
          "width": 4,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "Thread Count by Service",
          "type": "timeseries",
          "requests": [
            {
              "q": "avg:jvm.threads.live{env:docker-local} by {service}",
              "display_type": "area"
            }
          ]
        },
        "layout": {
          "x": 0,
          "y": 3,
          "width": 6,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "CPU Usage by Service",
          "type": "toplist",
          "requests": [
            {
              "q": "top(avg:process.runtime.jvm.cpu.utilization{env:docker-local} by {service}, 10, '\''mean'\'', '\''desc'\'')"
            }
          ]
        },
        "layout": {
          "x": 6,
          "y": 3,
          "width": 6,
          "height": 3
        }
      }
    ],
    "layout_type": "ordered",
    "is_read_only": false,
    "notify_list": []
  }' 2>&1 | grep -q '"id"' && echo "  ✅ JVM Metrics Dashboard created" || echo "  ⚠️  JVM Dashboard creation failed or already exists"

# Dashboard 2: Kafka Metrics
echo "  Creating Kafka Metrics Dashboard..."
curl -X POST "${API_BASE}/dashboard" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d '{
    "title": "Kafka Metrics Dashboard",
    "description": "Dashboard especializado en métricas de Kafka",
    "widgets": [
      {
        "definition": {
          "title": "Kafka Consumer Lag by Service",
          "type": "timeseries",
          "requests": [
            {
              "q": "avg:kafka.consumer.lag{env:docker-local} by {service,topic}",
              "display_type": "line"
            }
          ],
          "markers": [
            {
              "value": "y = 500",
              "display_type": "warning dashed"
            },
            {
              "value": "y = 1000",
              "display_type": "error dashed"
            }
          ]
        },
        "layout": {
          "x": 0,
          "y": 0,
          "width": 6,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "Messages Consumed Rate",
          "type": "timeseries",
          "requests": [
            {
              "q": "sum:kafka.consumer.records.consumed{env:docker-local} by {service}.as_rate()",
              "display_type": "bars"
            }
          ]
        },
        "layout": {
          "x": 6,
          "y": 0,
          "width": 6,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "Messages Produced Rate",
          "type": "timeseries",
          "requests": [
            {
              "q": "sum:kafka.producer.record.send.total{env:docker-local} by {service,topic}.as_rate()",
              "display_type": "bars"
            }
          ]
        },
        "layout": {
          "x": 0,
          "y": 3,
          "width": 6,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "Consumer Processing Time (P95)",
          "type": "timeseries",
          "requests": [
            {
              "q": "avg:spring.kafka.listener.seconds{quantile:0.95,env:docker-local} by {service}",
              "display_type": "line"
            }
          ]
        },
        "layout": {
          "x": 6,
          "y": 3,
          "width": 6,
          "height": 3
        }
      }
    ],
    "layout_type": "ordered",
    "is_read_only": false,
    "notify_list": []
  }' 2>&1 | grep -q '"id"' && echo "  ✅ Kafka Metrics Dashboard created" || echo "  ⚠️  Kafka Dashboard creation failed or already exists"

# Dashboard 3: Business Metrics
echo "  Creating Business Metrics Dashboard..."
curl -X POST "${API_BASE}/dashboard" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d '{
    "title": "Business Metrics - Crypto & News",
    "description": "Dashboard de métricas de negocio específicas del sistema",
    "widgets": [
      {
        "definition": {
          "title": "HTTP Requests per Minute",
          "type": "timeseries",
          "requests": [
            {
              "q": "sum:trace.servlet.request.hits{env:docker-local} by {service}.as_rate()",
              "display_type": "bars"
            }
          ]
        },
        "layout": {
          "x": 0,
          "y": 0,
          "width": 6,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "HTTP Request Latency (P95)",
          "type": "timeseries",
          "requests": [
            {
              "q": "avg:trace.servlet.request.duration{env:docker-local} by {service,resource_name}.rollup(avg, 60)",
              "display_type": "line"
            }
          ]
        },
        "layout": {
          "x": 6,
          "y": 0,
          "width": 6,
          "height": 3
        }
      },
      {
        "definition": {
          "title": "HTTP Error Rate",
          "type": "query_value",
          "requests": [
            {
              "q": "sum:trace.servlet.request.errors{env:docker-local}.as_rate()",
              "aggregator": "sum"
            }
          ],
          "precision": 2,
          "autoscale": true
        },
        "layout": {
          "x": 0,
          "y": 3,
          "width": 3,
          "height": 2
        }
      },
      {
        "definition": {
          "title": "Crypto Prices Fetched",
          "type": "query_value",
          "requests": [
            {
              "q": "sum:kafka.producer.record.send.total{service:crypto-fetcher-service,topic:crypto-prices,env:docker-local}.as_count()",
              "aggregator": "sum"
            }
          ],
          "precision": 0
        },
        "layout": {
          "x": 3,
          "y": 3,
          "width": 3,
          "height": 2
        }
      },
      {
        "definition": {
          "title": "Services Health",
          "type": "check_status",
          "check": "datadog.agent.up",
          "grouping": "cluster",
          "group_by": ["service"],
          "tags": ["env:docker-local"]
        },
        "layout": {
          "x": 6,
          "y": 3,
          "width": 6,
          "height": 2
        }
      }
    ],
    "layout_type": "ordered",
    "is_read_only": false,
    "notify_list": []
  }' 2>&1 | grep -q '"id"' && echo "  ✅ Business Metrics Dashboard created" || echo "  ⚠️  Business Dashboard creation failed or already exists"

echo ""
echo "🚨 Creating Monitors..."

# Monitor 1: High Error Rate
echo "  Creating High Error Rate Monitor..."
curl -X POST "${API_BASE}/monitor" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d '{
    "name": "[Microservices] High Error Rate",
    "type": "metric alert",
    "query": "sum(last_5m):sum:trace.servlet.request.errors{env:docker-local}.as_rate() by {service} > 10",
    "message": "⚠️ High Error Rate Detected on {{service.name}}. Error rate: {{value}} errors/sec",
    "tags": ["env:docker-local", "team:backend", "priority:high"],
    "options": {
      "thresholds": {
        "critical": 10,
        "warning": 5
      },
      "notify_no_data": false,
      "notify_audit": false
    }
  }' 2>&1 | grep -q '"id"' && echo "  ✅ High Error Rate Monitor created" || echo "  ⚠️  Monitor creation failed or already exists"

# Monitor 2: Consumer Lag
echo "  Creating Consumer Lag Monitor..."
curl -X POST "${API_BASE}/monitor" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d '{
    "name": "[Kafka] Consumer Lag Too High",
    "type": "metric alert",
    "query": "avg(last_10m):avg:kafka.consumer.lag{env:docker-local} by {service,topic} > 1000",
    "message": "⚠️ Kafka Consumer Lag Alert on {{service.name}} for topic {{topic.name}}. Current lag: {{value}} messages",
    "tags": ["env:docker-local", "component:kafka", "priority:critical"],
    "options": {
      "thresholds": {
        "critical": 1000,
        "warning": 500
      },
      "notify_no_data": true,
      "no_data_timeframe": 20
    }
  }' 2>&1 | grep -q '"id"' && echo "  ✅ Consumer Lag Monitor created" || echo "  ⚠️  Monitor creation failed or already exists"

# Monitor 3: Memory Usage
echo "  Creating Memory Usage Monitor..."
curl -X POST "${API_BASE}/monitor" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d '{
    "name": "[JVM] Memory Usage Above 90%",
    "type": "metric alert",
    "query": "avg(last_15m):(avg:jvm.memory.used{memory:heap,env:docker-local} by {service} / avg:jvm.memory.max{memory:heap,env:docker-local} by {service}) * 100 > 90",
    "message": "🔴 Critical: High Memory Usage on {{service.name}}. Memory usage: {{value}}%",
    "tags": ["env:docker-local", "component:jvm", "priority:critical"],
    "options": {
      "thresholds": {
        "critical": 90,
        "warning": 80
      },
      "notify_no_data": false
    }
  }' 2>&1 | grep -q '"id"' && echo "  ✅ Memory Usage Monitor created" || echo "  ⚠️  Monitor creation failed or already exists"

echo ""
echo "✅ Datadog setup completed!"
echo ""
echo "📊 Access your dashboards at: https://app.${DD_SITE}/dashboard/lists"
echo "🚨 Access your monitors at: https://app.${DD_SITE}/monitors/manage"
echo "🗺️  Access Service Map at: https://app.${DD_SITE}/apm/map?env=docker-local"
echo ""
