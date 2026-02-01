package com.alexlondon07.crypto_api.service;

import com.alexlondon07.crypto_api.model.CryptoPrice;
import com.alexlondon07.crypto_api.model.PriceStats;
import com.alexlondon07.crypto_api.repository.CryptoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CryptoService {
    private final CryptoRepository cryptoRepository;

    public Flux<CryptoPrice> getAllPrices() {
        return cryptoRepository.findAllCurrentPrices();
    }

    public Mono<CryptoPrice> getPriceBySymbol(String symbol) {
        return cryptoRepository.findCurrentPrice(symbol);
    }

    public Mono<PriceStats> getStatsBySymbol(String symbol) {
        return cryptoRepository.findStats(symbol);
    }
}
