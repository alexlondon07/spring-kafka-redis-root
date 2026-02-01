package com.alexlondon07.crypto_api.repository;

import com.alexlondon07.crypto_api.model.CryptoPrice;
import com.alexlondon07.crypto_api.model.PriceStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class CryptoRepositoryImp  implements CryptoRepository{
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_CURRENT = "crypto:current:";
    private static final String REDIS_KEY_STATS = "crypto:stats:";

    @Override
    public Mono<CryptoPrice> findCurrentPrice(String symbol) {
        return redisTemplate.opsForValue()
                .get(REDIS_KEY_CURRENT + symbol.toUpperCase())
                .map(obj -> objectMapper.convertValue(obj, CryptoPrice.class));
    }

    @Override
    public Flux<CryptoPrice> findAllCurrentPrices() {
        return redisTemplate.keys(REDIS_KEY_CURRENT + "*")
                .flatMap(key -> redisTemplate.opsForValue().get(key))
                .map(obj -> objectMapper.convertValue(obj, CryptoPrice.class));
    }

    @Override
    public Mono<PriceStats> findStats(String symbol) {
        return redisTemplate.opsForValue()
                .get(REDIS_KEY_STATS + symbol.toUpperCase())
                .map(obj -> objectMapper.convertValue(obj, PriceStats.class));
    }

}
