package com.alexlondon07.crypto_api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceStats {
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal avgPrice;
    private int sampleCount;
    private Instant lastUpdated;
}