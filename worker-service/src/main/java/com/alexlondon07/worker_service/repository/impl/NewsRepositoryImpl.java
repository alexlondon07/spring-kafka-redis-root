package com.alexlondon07.worker_service.repository.impl;

import com.alexlondon07.worker_service.repository.NewsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Repository
@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepository {

    private final ReactiveRedisOperations<String, Object> redisOperations;

    @Override
    public Mono<Boolean> saveNews(String date, Object newsObject) throws JsonProcessingException {
        Duration ttl = Duration.ofHours(1L);
        ObjectMapper objectMapper = new ObjectMapper();
        return redisOperations.opsForValue()
                .set(date, objectMapper.readTree(newsObject.toString()), ttl);
    }
}
