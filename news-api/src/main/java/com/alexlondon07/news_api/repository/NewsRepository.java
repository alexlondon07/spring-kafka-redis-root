package com.alexlondon07.news_api.repository;

import reactor.core.publisher.Mono;

public interface NewsRepository {
    Mono<Object> getNews(String date);
}
