package com.alexlondon07.crypto_api.repository;

import com.alexlondon07.crypto_api.model.CryptoPrice;
import com.alexlondon07.crypto_api.model.PriceStats;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CryptoRepository {

    Mono<CryptoPrice> findCurrentPrice(String symbol);

    Flux<CryptoPrice> findAllCurrentPrices();

    Mono<PriceStats> findStats(String symbol);
}
