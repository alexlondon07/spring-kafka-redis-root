package com.alexlondon07.news_api.repository.impl;

import com.alexlondon07.news_api.config.CustomDatadogConfig;
import com.alexlondon07.news_api.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class NewsRepositoryImpl implements NewsRepository {

    private final ReactiveRedisOperations<String, Object> redisOperations;
    private final CustomDatadogConfig.RedisMetricsCollector redisMetricsCollector;

    public NewsRepositoryImpl(ReactiveRedisOperations<String, Object> redisOperations, 
                            CustomDatadogConfig.RedisMetricsCollector redisMetricsCollector) {
        this.redisOperations = redisOperations;
        this.redisMetricsCollector = redisMetricsCollector;
    }

    @Override
    public Mono<Object> getNews(String date) {
        return redisMetricsCollector.timeRedisGet(
            redisOperations.opsForValue().get(date)
                .doOnSuccess(value -> {
                    if (value != null) {
                        redisMetricsCollector.recordCacheHit();
                    } else {
                        redisMetricsCollector.recordCacheMiss();
                    }
                })
                .doOnError(error -> redisMetricsCollector.recordCacheError())
        );
    }
}
