package com.alexlondon07.news_api.service;

import reactor.core.publisher.Mono;

public interface NewsService {

    Mono<Void> publishToMessageBroker(String date);

    Mono<Object> getNewsFromRedis(String date);
}
