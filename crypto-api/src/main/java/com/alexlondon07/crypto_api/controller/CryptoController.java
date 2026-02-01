package com.alexlondon07.crypto_api.controller;

import com.alexlondon07.crypto_api.model.CryptoPrice;
import com.alexlondon07.crypto_api.model.PriceStats;
import com.alexlondon07.crypto_api.service.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/crypto")
@RequiredArgsConstructor
@Slf4j
public class CryptoController {

    private final CryptoService cryptoService;

    @GetMapping("/prices")
    public Flux<CryptoPrice> getAllPrices() {
        log.info("Request: Get all crypto prices");
        return cryptoService.getAllPrices();
    }

    @GetMapping("/prices/{symbol}")
    public Mono<ResponseEntity<CryptoPrice>> getPrice(@PathVariable String symbol) {
        return cryptoService.getPriceBySymbol(symbol)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats/{symbol}")
    public Mono<ResponseEntity<PriceStats>> getStats(@PathVariable String symbol) {
        return cryptoService.getStatsBySymbol(symbol)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}