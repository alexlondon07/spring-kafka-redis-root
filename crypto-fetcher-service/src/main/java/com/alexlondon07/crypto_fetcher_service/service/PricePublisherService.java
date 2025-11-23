package com.alexlondon07.crypto_fetcher_service.service;

import com.alexlondon07.crypto_fetcher_service.model.CryptoPrice;
import com.alexlondon07.crypto_fetcher_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
@Slf4j
public class PricePublisherService {

    private final KafkaTemplate<String, CryptoPrice> kafkaTemplate;

    /**
     * Publishes a CryptoPrice message to the Kafka topic.
     * @param cryptoPrice The CryptoPrice object to be published.
     */
    public void publishPrice(CryptoPrice cryptoPrice) {
        String key = cryptoPrice.getSymbol();

        CompletableFuture<SendResult<String, CryptoPrice>> future =
                kafkaTemplate.send(Constants.TOPIC_CRYPTO_PRICES, key, cryptoPrice);

        future.whenComplete((result, ex) -> {
           if(ex == null){
                log.info("Published price for {} in usd {} to topic {} at offset {}",
                          cryptoPrice.getSymbol(),
                          cryptoPrice.getPriceUsd(),
                          result.getRecordMetadata().topic(),
                          result.getRecordMetadata().topic());
              } else {
                log.error("Failed to publish price for {}: {}: {}",
                          cryptoPrice.getSymbol(),
                          cryptoPrice.getPriceUsd(),
                          ex.getMessage());
           }

        });
    }
}
