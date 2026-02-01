package com.alexlondon07.alert_service.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CryptoPrice {
    private String symbol;
    private String name;
    private BigDecimal priceUsd;
    private BigDecimal priceChange24h;
    private BigDecimal marketCap;
    private Instant timestamp;
}
