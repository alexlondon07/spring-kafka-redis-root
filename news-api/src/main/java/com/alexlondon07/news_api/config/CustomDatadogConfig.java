package com.alexlondon07.news_api.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
public class CustomDatadogConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "service", System.getenv().getOrDefault("DD_SERVICE", "news-api"),
            "env", System.getenv().getOrDefault("DD_ENV", "docker-local"),
            "version", System.getenv().getOrDefault("DD_VERSION", "1.0.0")
        );
    }

    @Bean
    public RedisMetricsCollector redisMetricsCollector(MeterRegistry meterRegistry) {
        return new RedisMetricsCollector(meterRegistry);
    }

    public static class RedisMetricsCollector {
        private final MeterRegistry meterRegistry;
        private final AtomicLong cacheHits = new AtomicLong(0);
        private final AtomicLong cacheMisses = new AtomicLong(0);
        private final AtomicLong cacheErrors = new AtomicLong(0);
        private final Timer redisGetTimer;
        private final Timer redisSetTimer;

        public RedisMetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.redisGetTimer = Timer.builder("redis.get.duration")
                .description("Time taken to get values from Redis")
                .tag("operation", "get")
                .register(meterRegistry);
            
            this.redisSetTimer = Timer.builder("redis.set.duration")
                .description("Time taken to set values in Redis")
                .tag("operation", "set")
                .register(meterRegistry);

            // Register counters for cache operations
            meterRegistry.gauge("redis.cache.hits", cacheHits);
            meterRegistry.gauge("redis.cache.misses", cacheMisses);
            meterRegistry.gauge("redis.cache.errors", cacheErrors);
        }

        public <T> reactor.core.publisher.Mono<T> timeRedisGet(reactor.core.publisher.Mono<T> operation) {
            Timer.Sample sample = Timer.start(meterRegistry);
            return operation
                .doOnSuccess(value -> {
                    if (value != null) {
                        cacheHits.incrementAndGet();
                        meterRegistry.counter("redis.cache.operations", "type", "hit").increment();
                    } else {
                        cacheMisses.incrementAndGet();
                        meterRegistry.counter("redis.cache.operations", "type", "miss").increment();
                    }
                })
                .doOnError(error -> {
                    cacheErrors.incrementAndGet();
                    meterRegistry.counter("redis.cache.operations", "type", "error").increment();
                })
                .doFinally(signalType -> sample.stop(redisGetTimer));
        }

        public <T> reactor.core.publisher.Mono<T> timeRedisSet(reactor.core.publisher.Mono<T> operation) {
            Timer.Sample sample = Timer.start(meterRegistry);
            return operation
                .doOnSuccess(value -> {
                    meterRegistry.counter("redis.cache.operations", "type", "set").increment();
                })
                .doOnError(error -> {
                    cacheErrors.incrementAndGet();
                    meterRegistry.counter("redis.cache.operations", "type", "error").increment();
                })
                .doFinally(signalType -> sample.stop(redisSetTimer));
        }

        public void recordCacheHit() {
            cacheHits.incrementAndGet();
            meterRegistry.counter("redis.cache.operations", "type", "hit").increment();
        }

        public void recordCacheMiss() {
            cacheMisses.incrementAndGet();
            meterRegistry.counter("redis.cache.operations", "type", "miss").increment();
        }

        public void recordCacheError() {
            cacheErrors.incrementAndGet();
            meterRegistry.counter("redis.cache.operations", "type", "error").increment();
        }
    }
}