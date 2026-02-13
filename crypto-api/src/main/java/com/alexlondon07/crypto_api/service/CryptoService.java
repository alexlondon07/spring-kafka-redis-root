package com.alexlondon07.crypto_api.service;

import com.alexlondon07.crypto_api.model.CryptoListResponse;
import com.alexlondon07.crypto_api.model.CryptoPrice;
import com.alexlondon07.crypto_api.model.PriceStats;
import com.alexlondon07.crypto_api.repository.CryptoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Service
public class CryptoService {
    
    private static final List<String> AVAILABLE_SYMBOLS = Arrays.asList("BTC", "ETH", "SOL");
    
    private final CryptoRepository cryptoRepository;

    public CryptoService(CryptoRepository cryptoRepository) {
        this.cryptoRepository = cryptoRepository;
    }

    public Flux<CryptoPrice> getAllPrices() {
        return cryptoRepository.findAllCurrentPrices();
    }

    public Mono<CryptoPrice> getPriceBySymbol(String symbol) {
        return cryptoRepository.findCurrentPrice(symbol);
    }

    public Mono<PriceStats> getStatsBySymbol(String symbol) {
        return cryptoRepository.findStats(symbol);
    }
    
    public Mono<CryptoListResponse> getAvailableSymbols() {
        return Mono.just(CryptoListResponse.builder()
                .symbols(AVAILABLE_SYMBOLS)
                .count(AVAILABLE_SYMBOLS.size())
                .message("Available cryptocurrency symbols")
                .build());
    }
}
