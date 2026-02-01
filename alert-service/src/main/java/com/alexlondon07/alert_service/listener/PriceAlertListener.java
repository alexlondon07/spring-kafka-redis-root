package com.alexlondon07.alert_service.listener;

import com.alexlondon07.alert_service.model.CryptoPrice;
import com.alexlondon07.alert_service.service.AlertDetectionService;
import com.alexlondon07.alert_service.service.AlertPublisherService;
import com.alexlondon07.alert_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAlertListener {

    private final AlertPublisherService alertPublisherService;
    private final AlertDetectionService alertDetectionService;

    @KafkaListener(
            topics = Constants.TOPIC_CRYPTO_PRICES,
            groupId = Constants.CONSUMER_GROUP
    )
    public void onPricerECEIVED(CryptoPrice price){
        log.debug("Price received for {} : ${}", price.getSymbol(), price.getPriceUsd());

        alertDetectionService.evaluatePrice(price)
                .ifPresent(alertPublisherService::publishAlert);
    }
}
