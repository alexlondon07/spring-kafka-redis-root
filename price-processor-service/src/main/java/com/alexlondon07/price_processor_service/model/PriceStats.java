package com.alexlondon07.price_processor_service.model;


import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceStats {
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal avgPrice;
    private int sampleCount;
    private Instant lastUpdated;
}
