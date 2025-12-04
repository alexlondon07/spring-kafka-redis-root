package com.alexlondon07.price_processor_service.repository;


import com.alexlondon07.price_processor_service.model.CryptoPrice;
import com.alexlondon07.price_processor_service.model.PriceStats;
import reactor.core.publisher.Mono;
public interface PriceRepository {

    Mono<Boolean> saveCurrentPrice(CryptoPrice price);

    Mono<CryptoPrice> getCurrentPrice(String symbol);

    Mono<Boolean> saveStats(PriceStats stats);

    Mono<PriceStats> getStats(String symbol);

    Mono<Long> addToHistory(String symbol, CryptoPrice price);
}
