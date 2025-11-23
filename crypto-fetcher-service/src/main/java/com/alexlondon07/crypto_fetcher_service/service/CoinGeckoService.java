package com.alexlondon07.crypto_fetcher_service.service;

import com.alexlondon07.crypto_fetcher_service.model.CryptoPrice;
import com.alexlondon07.crypto_fetcher_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoService {

    private final WebClient webClient;

    /**
     * Fetches cryptocurrency prices from the CoinGecko API.
     * API: GET
     * /simple/price?ids=bitcoin,ethereum&vs_currencies=usd&include_market_cap=true&include_24hr_change=true
     * @return A Flux stream of CryptoPrice objects.
     */
    public Flux<CryptoPrice> fetchCryptoPrices() {

        String ids = String.join(",", Constants.CRYPTO_IDS);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/simple/price")
                        .queryParam("ids", ids)
                        .queryParam("vs_currencies", "usd")
                        .queryParam("include_market_cap", "true")
                        .queryParam("include_24hr_change", "true")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response -> log.debug("Received response from CoinGecko: {}", response))
                .flatMapMany(this::mapToCryptoPrices)
                .doOnError(error -> log.error("Error fetching prices from CoinGecko", error));
    }

    /**
     * Maps the API response to a Flux of CryptoPrice objects.
     * @param response
     * @return A Flux stream of CryptoPrice objects.
     * Example response:
     * {
     *   "bitcoin": {
     *     "usd": 43000.50,
     *     "usd_market_cap": 840000000000,
     *     "usd_24h_change": 2.5
     *   }
     */
    private Flux<CryptoPrice> mapToCryptoPrices(Map<String, Map<String, Object>> response) {

        return Flux.fromIterable(response.entrySet())
                .map(entry -> {
                    String id = entry.getKey();
                    Map<String, Object> data = entry.getValue();

                    return CryptoPrice.builder()
                            .symbol(getSymbol(id))
                            .name(capitalize(id))
                            .priceUsd(toBigDecimal(data.get("usd")))
                            .priceChange24h(toBigDecimal(data.get("usd_24h_change")))
                            .marketCap(toBigDecimal(data.get("usd_market_cap")))
                            .timestamp(Instant.now())
                            .build();
                });
    }

    private String getSymbol(String id) {
        return switch (id) {
            case "bitcoin" -> Constants.BTC;
            case "ethereum" -> Constants.ETH;
            case "solana" -> Constants.SOL;
            default -> id.toUpperCase();
        };
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
