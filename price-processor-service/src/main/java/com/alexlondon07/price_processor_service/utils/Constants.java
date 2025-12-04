package com.alexlondon07.price_processor_service.utils;

public class Constants {

    // Kafka
    public static final String TOPIC_CRYPTO_PRICES = "crypto-prices";
    public static final String CONSUMER_GROUP = "price-processor-group";

    // Redis keys
    public static final String REDIS_KEY_CURRENT = "crypto:current:";  // crypto:current:BTC
    public static final String REDIS_KEY_STATS = "crypto:stats:";      // crypto:stats:BTC
    public static final String REDIS_KEY_HISTORY = "crypto:history:";  // crypto:history:BTC

    private Constants() {}
}
