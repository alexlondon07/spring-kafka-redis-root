package com.alexlondon07.alert_service.model;

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
public class PriceAlert {
    private String symbol;
    private String alertType;        // PRICE_INCREASE, PRICE_DECREASE
    private BigDecimal previousPrice;
    private BigDecimal currentPrice;
    private BigDecimal changePercent;
    private Instant timestamp;
    private String message;
}
