package com.alexlondon07.price_processor_service.listener;

import com.alexlondon07.price_processor_service.model.CryptoPrice;
import com.alexlondon07.price_processor_service.service.PriceStorageService;
import com.alexlondon07.price_processor_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceListener {

    private final PriceStorageService priceStorageService;

    @KafkaListener(
            topics = Constants.TOPIC_CRYPTO_PRICES,
            groupId = Constants.CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPriceReceived(ConsumerRecord<String, CryptoPrice> record){
        String key = record.key();
        CryptoPrice price = record.value();

        int partition = record.partition();
        long offset = record.offset();

        // Handle deserialization errors
        if (price == null) {
            log.error("Deserialization error for message - Key: {}, Partition: {}, Offset: {}. Skipping message.",
                    key, partition, offset);
            return;
        }

        log.info("Received price for {}: {}", key, price);
        log.info("Record details - Partition: {}, Offset: {}", partition, offset);

        priceStorageService.processPrice(price)
                .subscribe(
                        null,
                        error -> log.error("Error processing price for {}: {}", key, error.getMessage()),
                        () -> log.info("Successfully processed price for {}", key)
                );

    }

}
