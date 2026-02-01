package com.alexlondon07.price_processor_service.listener;

import com.alexlondon07.price_processor_service.config.CustomDatadogConfig;
import com.alexlondon07.price_processor_service.model.CryptoPrice;
import com.alexlondon07.price_processor_service.service.PriceStorageService;
import com.alexlondon07.price_processor_service.utils.Constants;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class PriceListener {

    private static final Logger log = LoggerFactory.getLogger(PriceListener.class);
    
    private final PriceStorageService priceStorageService;
    private final CustomDatadogConfig.PriceMetricsCollector priceMetricsCollector;
    private final MeterRegistry meterRegistry;

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
            meterRegistry.counter("kafka.consumer.errors", 
                    "service", "price-processor-service",
                    "error_type", "deserialization").increment();
            priceMetricsCollector.recordPriceError();
            log.error("Deserialization error for message - Key: {}, Partition: {}, Offset: {}. Skipping message.",
                    key, partition, offset);
            return;
        }

        log.info("Received price for {}: {}", key, price);
        log.info("Record details - Partition: {}, Offset: {}", partition, offset);

        // Record Kafka consumption metrics
        meterRegistry.counter("kafka.consumer.messages", 
                "service", "price-processor-service",
                "topic", Constants.TOPIC_CRYPTO_PRICES).increment();

        priceMetricsCollector.timeKafkaProcessing(
            priceStorageService.processPrice(price)
                .doOnSuccess(v -> {
                    meterRegistry.counter("price.storage.success", 
                            "symbol", price.getSymbol()).increment();
                })
                .doOnError(error -> {
                    meterRegistry.counter("price.storage.error", 
                            "symbol", price.getSymbol(),
                            "error_type", error.getClass().getSimpleName()).increment();
                })
        ).subscribe(
                null,
                error -> {
                    priceMetricsCollector.recordPriceError();
                    log.error("Error processing price for {}: {}", key, error.getMessage());
                },
                () -> {
                    priceMetricsCollector.recordPriceProcessed();
                    log.info("Successfully processed price for {}", key);
                }
        );
    }

}
