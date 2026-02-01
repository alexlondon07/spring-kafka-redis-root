package com.alexlondon07.alert_service.service;


import com.alexlondon07.alert_service.model.PriceAlert;
import com.alexlondon07.alert_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertPublisherService {
    private final KafkaTemplate<String, PriceAlert> kafkaTemplate;

    public void publishAlert(PriceAlert alert) {
        kafkaTemplate.send(Constants.TOPIC_PRICE_ALERTS, alert.getSymbol(), alert)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error(" Failed to publish alert for {}: {}", alert.getSymbol(), ex.getMessage());
                            } else {
                                log.info("Published alert: {}", alert.getMessage());
                            }
                        });
    }


}
