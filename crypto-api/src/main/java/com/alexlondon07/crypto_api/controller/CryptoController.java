package com.alexlondon07.crypto_api.controller;

import com.alexlondon07.crypto_api.model.CryptoListResponse;
import com.alexlondon07.crypto_api.model.CryptoPrice;
import com.alexlondon07.crypto_api.model.PriceStats;
import com.alexlondon07.crypto_api.service.CryptoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Slf4j
@Tag(name = "Cryptocurrency API", description = "API for retrieving cryptocurrency prices, statistics and historical data")
public class CryptoController {

    private final CryptoService cryptoService;

    public CryptoController(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Operation(summary = "Get available cryptocurrencies", description = "Retrieves the list of available cryptocurrency symbols that can be tracked")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CryptoListResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/symbols")
    public Mono<ResponseEntity<CryptoListResponse>> getAvailableSymbols() {
        log.info("Request: Get available cryptocurrency symbols");
        return cryptoService.getAvailableSymbols()
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Get all cryptocurrency prices", description = "Retrieves current prices for all tracked cryptocurrencies (BTC, ETH, SOL)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all prices",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CryptoPrice.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/prices")
    public Flux<CryptoPrice> getAllPrices() {
        log.info("Request: Get all crypto prices");
        return cryptoService.getAllPrices();
    }

    @Operation(summary = "Get price by symbol", description = "Retrieves current price for a specific cryptocurrency by its symbol")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved price",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CryptoPrice.class))),
            @ApiResponse(responseCode = "404", description = "Cryptocurrency not found")
    })
    @GetMapping("/prices/{symbol}")
    public Mono<ResponseEntity<CryptoPrice>> getPrice(
            @Parameter(description = "Cryptocurrency symbol (e.g., BTC, ETH, SOL)", required = true, example = "BTC")
            @PathVariable String symbol) {
        return cryptoService.getPriceBySymbol(symbol)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get price statistics", description = "Retrieves price statistics (min, max, avg, sample count) for a specific cryptocurrency")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PriceStats.class))),
            @ApiResponse(responseCode = "404", description = "Cryptocurrency not found")
    })
    @GetMapping("/stats/{symbol}")
    public Mono<ResponseEntity<PriceStats>> getStats(
            @Parameter(description = "Cryptocurrency symbol (e.g., BTC, ETH, SOL)", required = true, example = "BTC")
            @PathVariable String symbol) {
        return cryptoService.getStatsBySymbol(symbol)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}