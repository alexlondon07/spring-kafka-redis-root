package com.alexlondon07.news_api.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.alexlondon07.news_api.utils.Constants.TOPIC_NAME;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic newsTopic(){
        return TopicBuilder.name(TOPIC_NAME).build();
    }
}
