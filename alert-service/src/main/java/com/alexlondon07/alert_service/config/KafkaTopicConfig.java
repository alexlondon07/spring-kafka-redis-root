package com.alexlondon07.alert_service.config;

import com.alexlondon07.alert_service.utils.Constants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic cryptoPricesTopic() {
        return TopicBuilder.name(Constants.TOPIC_CRYPTO_PRICES)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
