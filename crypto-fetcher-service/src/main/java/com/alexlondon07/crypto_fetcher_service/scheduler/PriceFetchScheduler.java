package com.alexlondon07.crypto_fetcher_service.scheduler;


import com.alexlondon07.crypto_fetcher_service.service.CoinGeckoService;
import com.alexlondon07.crypto_fetcher_service.service.PricePublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceFetchScheduler {

    private final CoinGeckoService coinGeckoService;
    private final PricePublisherService pricePublisherService;

    /**
     * Scheduled task to fetch cryptocurrency prices from CoinGecko
     * and publish them to a Kafka topic at fixed intervals.
     * The interval is configurable via application properties.
     */
    @Scheduled(fixedRateString = "${scheduler.fetch-interval}", initialDelay = 0)
    public void fetchAndPublishPrices() {
        log.info("Starting scheduled task to fetch and publish crypto prices.");

        coinGeckoService.fetchCryptoPrices()
                .doOnNext(price -> log.debug("Fetched: {} = ${}", price.getSymbol(), price.getPriceUsd()))
                .subscribe(
                        pricePublisherService::publishPrice,
                        error -> log.error("Error in price fetch schedule", error),
                        () -> log.info("Completed price fetch cycle")
                );
    }
}
