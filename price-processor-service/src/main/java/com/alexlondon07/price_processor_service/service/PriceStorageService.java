package com.alexlondon07.price_processor_service.service;

import com.alexlondon07.price_processor_service.config.CustomDatadogConfig;
import com.alexlondon07.price_processor_service.model.CryptoPrice;
import com.alexlondon07.price_processor_service.model.PriceStats;
import com.alexlondon07.price_processor_service.repository.PriceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@Slf4j
public class PriceStorageService {

    private final PriceRepository priceRepository;
    private final CustomDatadogConfig.PriceMetricsCollector priceMetricsCollector;
    private final MeterRegistry meterRegistry;

    public PriceStorageService(PriceRepository priceRepository, 
                           CustomDatadogConfig.PriceMetricsCollector priceMetricsCollector,
                           MeterRegistry meterRegistry) {
        this.priceRepository = priceRepository;
        this.priceMetricsCollector = priceMetricsCollector;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Process and store the incoming crypto price.
     * Saves the current price, adds it to history, and updates statistics.
     * @param price CryptoPrice object containing the price details.
     * @return Mono<Void>
     */
    public Mono<Void> processPrice(CryptoPrice price) {
        return priceMetricsCollector.timePriceProcessing(
            priceRepository.saveCurrentPrice(price)
                .then( priceRepository.addToHistory(price.getSymbol(), price))
                .then(updateStats(price))
                .then()
                .doOnSuccess(v -> {
                    log.info("Processed price for {}: {}", price.getSymbol(), price);
                    meterRegistry.counter("price.storage.success", "symbol", price.getSymbol()).increment();
                })
                .doOnError(error -> {
                    log.error("Error processing price for {}: {}", price.getSymbol(), error.getMessage());
                    
                    // Handle Jackson deserialization errors specifically
                    if (error.getMessage() != null && error.getMessage().contains("Java 8 date/time type")) {
                        log.error("Jackson JSR310 deserialization error for {}: {}", price.getSymbol(), error.getMessage());
                        meterRegistry.counter("price.storage.error", 
                                "symbol", price.getSymbol(),
                                "error_type", "JacksonDeserializationError").increment();
                    } else {
                        meterRegistry.counter("price.storage.error", 
                                "symbol", price.getSymbol(),
                                "error_type", error.getClass().getSimpleName()).increment();
                    }
                })
        );
                
    }

    /**
     * Update the price statistics with the new price.
     * @param price CryptoPrice object containing the new price details.
     * @return Mono<Object> Updated PriceStats object.
     */
    private Mono<Object> updateStats(CryptoPrice price) {
        return priceRepository.getStats(price.getSymbol())
                .defaultIfEmpty(createInitialStats(price))
                .flatMap(oldStats -> {
                    PriceStats updatedStats = updateStatsWithNewPrice(oldStats, price);
                    
                    // Record price change metrics
                    priceMetricsCollector.recordPriceStats(
                        price.getSymbol(),
                        price.getPriceUsd(),
                        updatedStats.getMinPrice(),
                        updatedStats.getMaxPrice(),
                        updatedStats.getAvgPrice(),
                        updatedStats.getSampleCount()
                    );
                    
                    // Check for price change alerts
                    if (oldStats.getSampleCount() > 0) {
                        BigDecimal oldPrice = oldStats.getCurrentPrice();
                        if (oldPrice != null && oldPrice.compareTo(BigDecimal.ZERO) > 0) {
                            priceMetricsCollector.recordPriceChange(
                                price.getSymbol(), oldPrice, price.getPriceUsd()
                            );
                        }
                    }
                    
                    return priceRepository.saveStats(updatedStats);
                });
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
