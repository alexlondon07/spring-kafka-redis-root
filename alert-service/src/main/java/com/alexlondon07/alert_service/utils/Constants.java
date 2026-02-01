package com.alexlondon07.alert_service.utils;

public class Constants {

    // Kafka Topics
    public static final String TOPIC_CRYPTO_PRICES = "crypto-prices";
    public static final String TOPIC_PRICE_ALERTS = "price-alerts";

    public static final String CONSUMER_GROUP = "alert-service-group";
    // Alert thresholds
    public static final double ALERT_THRESHOLD_PERCENT = 5.0;

    // Alert types
    public static final String ALERT_PRICE_INCREASE = "PRICE_INCREASE";
    public static final String ALERT_PRICE_DECREASE = "PRICE_DECREASE";

    private Constants() {}
}