package com.alexlondon07.price_processor_service.repository;

import com.alexlondon07.price_processor_service.model.CryptoPrice;
import com.alexlondon07.price_processor_service.model.PriceStats;
import com.alexlondon07.price_processor_service.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
@RequiredArgsConstructor
public class PriceRepositoryImpl implements PriceRepository {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Boolean> saveCurrentPrice(CryptoPrice price) {
        String key = Constants.REDIS_KEY_CURRENT + price.getSymbol();
        return redisTemplate.opsForValue()
                .set(key, price)
                .doOnSuccess(success -> log.info("Saved current price for {}: {}", price.getSymbol(), price))
                .doOnError(error -> log.error("Error saving current price for {}: {}", price.getSymbol(), error.getMessage()));
    }

    @Override
    public Mono<CryptoPrice> getCurrentPrice(String symbol) {
        String key = Constants.REDIS_KEY_CURRENT + symbol;
        return redisTemplate.opsForValue()
                .get(key)
                .map(value -> objectMapper.convertValue(value, CryptoPrice.class))
                .doOnSuccess(price -> log.info("Retrieved current price for {}: {}", symbol, price))
                .doOnError(error -> log.error("Error retrieving current price for {}: {}", symbol, error.getMessage()));
    }

    @Override
    public Mono<Boolean> saveStats(PriceStats stats) {
        String key = Constants.REDIS_KEY_CURRENT + stats.getSymbol();
        return redisTemplate.opsForValue()
                .set(key, stats)
                .doOnSuccess(success -> log.info("Saved stats for {}: {}", stats.getSymbol(), stats))
                .doOnError(error -> log.error("Error saving stats for {}: {}", stats.getSymbol(), error.getMessage()));
    }

    @Override
    public Mono<PriceStats> getStats(String symbol) {
        String key = Constants.REDIS_KEY_STATS + symbol;
        return redisTemplate.opsForValue()
                .get(key)
                .map(value -> objectMapper.convertValue(value, PriceStats.class))
                .doOnSuccess(stats -> log.info("Retrieved stats for {}: {}", symbol, stats))
                .doOnError(error -> log.error("Error retrieving stats for {}: {}", symbol, error.getMessage()));
    }

    @Override
    public Mono<Long> addToHistory(String symbol, CryptoPrice price) {
        String key = Constants.REDIS_KEY_HISTORY + symbol;
        return redisTemplate.opsForList()
                .rightPush(key, price)
                .doOnSuccess(size -> log.info("Added to history for {}: {}, new size: {}", symbol, price, size))
                .doOnError(error -> log.error("Error adding to history for {}: {}", symbol, error.getMessage()));
    }
}
