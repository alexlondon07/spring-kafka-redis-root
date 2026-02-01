package com.alexlondon07.price_processor_service.config;

import com.alexlondon07.price_processor_service.utils.Constants;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
public class CustomDatadogConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "service", System.getenv().getOrDefault("DD_SERVICE", "price-processor-service"),
            "env", System.getenv().getOrDefault("DD_ENV", "docker-local"),
            "version", System.getenv().getOrDefault("DD_VERSION", "1.0.0")
        );
    }

    @Bean
    public PriceMetricsCollector priceMetricsCollector(MeterRegistry meterRegistry) {
        return new PriceMetricsCollector(meterRegistry);
    }

    public static class PriceMetricsCollector {
        private final MeterRegistry meterRegistry;
        private final AtomicLong pricesProcessed = new AtomicLong(0);
        private final AtomicLong priceErrors = new AtomicLong(0);
        private final AtomicLong priceAlerts = new AtomicLong(0);
        private final Timer priceProcessingTimer;
        private final Timer kafkaProcessingTimer;

        public PriceMetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.priceProcessingTimer = Timer.builder("price.processing.duration")
                .description("Time taken to process crypto prices")
                .tag("operation", "process")
                .register(meterRegistry);
            
            this.kafkaProcessingTimer = Timer.builder("kafka.consumer.duration")
                .description("Time taken to process Kafka messages")
                .tag("operation", "consume")
                .register(meterRegistry);

            // Register counters
            meterRegistry.gauge("price.processed.count", pricesProcessed);
            meterRegistry.gauge("price.errors.count", priceErrors);
            meterRegistry.gauge("price.alerts.count", priceAlerts);
        }

        public <T> reactor.core.publisher.Mono<T> timePriceProcessing(reactor.core.publisher.Mono<T> operation) {
            Timer.Sample sample = Timer.start(meterRegistry);
            return operation
                .doOnSuccess(value -> {
                    pricesProcessed.incrementAndGet();
                    meterRegistry.counter("price.processing.operations", "type", "success").increment();
                })
                .doOnError(error -> {
                    priceErrors.incrementAndGet();
                    meterRegistry.counter("price.processing.operations", "type", "error").increment();
                })
                .doFinally(signalType -> sample.stop(priceProcessingTimer));
        }

        public <T> reactor.core.publisher.Mono<T> timeKafkaProcessing(reactor.core.publisher.Mono<T> operation) {
            Timer.Sample sample = Timer.start(meterRegistry);
            return operation
                .doOnSuccess(value -> {
                    meterRegistry.counter("kafka.consumer.operations", "type", "success").increment();
                })
                .doOnError(error -> {
                    meterRegistry.counter("kafka.consumer.operations", "type", "error").increment();
                })
                .doFinally(signalType -> sample.stop(kafkaProcessingTimer));
        }

        public void recordPriceChange(String symbol, BigDecimal oldPrice, BigDecimal newPrice) {
            double changePercent = newPrice.subtract(oldPrice)
                    .divide(oldPrice, 4, java.math.RoundingMode.HALF_UP)
                    .doubleValue() * 100;
            
            // Record price change as a gauge
            meterRegistry.gauge("price.change.percent", Tags.of("symbol", symbol), changePercent);
            
            // Check for significant changes (alerts)
            if (Math.abs(changePercent) > Constants.ALERT_THRESHOLD_PERCENT) { // 1% threshold
                priceAlerts.incrementAndGet();
                meterRegistry.counter("price.alerts", 
                    "symbol", symbol, 
                    "type", changePercent > 0 ? "increase" : "decrease").increment();
                
                System.out.println("Price alert for " + symbol + ": " + changePercent + "% change from " + oldPrice + " to " + newPrice);
            }
            
            // Record absolute price
            meterRegistry.gauge("price.current", Tags.of("symbol", symbol), newPrice.doubleValue());
        }

        public void recordPriceStats(String symbol, BigDecimal currentPrice, BigDecimal minPrice, 
                                   BigDecimal maxPrice, BigDecimal avgPrice, int sampleCount) {
            meterRegistry.gauge("price.current", Tags.of("symbol", symbol), currentPrice.doubleValue());
            meterRegistry.gauge("price.min", Tags.of("symbol", symbol), minPrice.doubleValue());
            meterRegistry.gauge("price.max", Tags.of("symbol", symbol), maxPrice.doubleValue());
            meterRegistry.gauge("price.avg", Tags.of("symbol", symbol), avgPrice.doubleValue());
            meterRegistry.gauge("price.samples", Tags.of("symbol", symbol), sampleCount);
            
            // Calculate and record volatility (range as percentage of current price)
            if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal range = maxPrice.subtract(minPrice);
                double volatility = range.divide(currentPrice, 4, java.math.RoundingMode.HALF_UP)
                        .doubleValue() * 100;
                meterRegistry.gauge("price.volatility", Tags.of("symbol", symbol), volatility);
            }
        }

        public void recordPriceProcessed() {
            pricesProcessed.incrementAndGet();
            meterRegistry.counter("price.processing.operations", "type", "success").increment();
        }

        public void recordPriceError() {
            priceErrors.incrementAndGet();
            meterRegistry.counter("price.processing.operations", "type", "error").increment();
        }

        public void recordPriceAlert(String symbol, String alertType) {
            priceAlerts.incrementAndGet();
            meterRegistry.counter("price.alerts", "symbol", symbol, "type", alertType).increment();
        }
    }
}