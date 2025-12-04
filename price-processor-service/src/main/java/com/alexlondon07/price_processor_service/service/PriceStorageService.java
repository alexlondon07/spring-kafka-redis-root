package com.alexlondon07.price_processor_service.service;


import com.alexlondon07.price_processor_service.model.CryptoPrice;
import com.alexlondon07.price_processor_service.model.PriceStats;
import com.alexlondon07.price_processor_service.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceStorageService {

    private final PriceRepository priceRepository;

    /**
     * Process and store the incoming crypto price.
     * Saves the current price, adds it to history, and updates statistics.
     * @param price CryptoPrice object containing the price details.
     * @return Mono<Void>
     */
    public Mono<Void> processPrice(CryptoPrice price) {
        return priceRepository.saveCurrentPrice(price)
                .then( priceRepository.addToHistory(price.getSymbol(), price))
                .then(updateStats(price))
                .then()
                .doOnSuccess(v -> log.info("Processed price for {}: {}", price.getSymbol(), price))
                .doOnError(error -> log.error("Error processing price for {}: {}", price.getSymbol(), error.getMessage()));
                
    }

    /**
     * Update the price statistics with the new price.
     * @param price CryptoPrice object containing the new price details.
     * @return Mono<Object> Updated PriceStats object.
     */
    private Mono<Object> updateStats(CryptoPrice price) {
        return priceRepository.getStats(price.getSymbol())
                .defaultIfEmpty(createInitialStats(price))
                .map(stats -> updateStatsWithNewPrice(stats, price))
                .flatMap(priceRepository::saveStats);
    }

    /**
     * Update existing PriceStats with a new CryptoPrice.
     * Calculate a new avg ( (oldAvg * oldCount) + newPrice ) / newCount
     * @param stats Existing PriceStats object.
     * @param price New CryptoPrice object.
     * @return Updated PriceStats object.
     */
    private PriceStats updateStatsWithNewPrice(PriceStats stats, CryptoPrice price) {
        BigDecimal newPrice = price.getPriceUsd();

        int newCount = stats.getSampleCount() + 1;
        BigDecimal newAvg = stats.getAvgPrice()
                .multiply(BigDecimal.valueOf(stats.getSampleCount()))
                .add(newPrice)
                .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        return PriceStats.builder()
                .symbol(stats.getSymbol())
                .currentPrice(newPrice)
                .minPrice(stats.getMinPrice().min(newPrice))
                .maxPrice(stats.getMaxPrice().max(newPrice))
                .avgPrice(newAvg)
                .sampleCount(newCount)
                .lastUpdated(Instant.now())
                .build();
        
    }

    private PriceStats createInitialStats(CryptoPrice price) {
        return PriceStats.builder()
                .symbol(price.getSymbol())
                .currentPrice(price.getPriceUsd())
                .minPrice(price.getPriceUsd())
                .maxPrice(price.getPriceUsd())
                .avgPrice(price.getPriceUsd())
                .sampleCount(0)
                .lastUpdated(Instant.now())
                .build();
    }
}
