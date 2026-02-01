package com.alexlondon07.alert_service.service;

import com.alexlondon07.alert_service.model.CryptoPrice;
import com.alexlondon07.alert_service.model.PriceAlert;
import com.alexlondon07.alert_service.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AlertDetectionService {

    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    public Optional<PriceAlert> evaluatePrice(CryptoPrice cryptoPrice) {

        String symbol = cryptoPrice.getSymbol();
        BigDecimal currentPrice = cryptoPrice.getPriceUsd();
        BigDecimal previousPrice = lastPrices.get(symbol);

        if (previousPrice == null) {
            log.debug(" First price for {}, no alert ", symbol);
            return Optional.empty();
        }

        // calculate percentage change
        BigDecimal change = currentPrice.subtract(previousPrice)
                .divide(previousPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        double changePercent = change.doubleValue();

        // validate if change exceeds threshold
        if(Math.abs(changePercent) >= Constants.ALERT_THRESHOLD_PERCENT) {
            String alertType = changePercent > 0 ? Constants.ALERT_PRICE_INCREASE : Constants.ALERT_PRICE_DECREASE;

            PriceAlert alert = PriceAlert.builder()
                    .symbol(symbol)
                    .alertType(alertType)
                    .previousPrice(currentPrice)
                    .changePercent(change.setScale(2, RoundingMode.HALF_UP))
                    .timestamp(Instant.now())
                    .message(String.format("%s %s %.2f%% from $%s to $%s",
                            symbol,
                            changePercent > 0 ? "increased" : "decreased",
                            Math.abs(changePercent),
                            previousPrice,
                            currentPrice))
                    .build();

            log.warn(" Alert generated: {} ", alert.getMessage());

            return Optional.of(alert);
        }



        return Optional.empty();
    }
}
