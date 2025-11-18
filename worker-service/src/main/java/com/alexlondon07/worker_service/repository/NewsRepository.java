package com.alexlondon07.worker_service.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface NewsRepository {
    Mono<Boolean> saveNews(String date, Object newObject) throws JsonProcessingException;
}
