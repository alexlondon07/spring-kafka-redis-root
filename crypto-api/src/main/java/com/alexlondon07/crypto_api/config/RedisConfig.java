package com.alexlondon07.crypto_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private String port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(Objects.requireNonNull(host));
        redisStandaloneConfiguration.setPort(Integer.parseInt(Objects.requireNonNull(port)));
        redisStandaloneConfiguration.setPassword(RedisPassword.of(Objects.requireNonNull(password)));
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        
        ObjectMapper objectMapper = objectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        
        RedisSerializationContext<String, Object> context =
                RedisSerializationContext.<String, Object>newSerializationContext(new StringRedisSerializer())
                        .value(serializer)
                        .build();
        
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }
    
    @Bean
    public ReactiveRedisOperations<String, Object> redisOperations(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return reactiveRedisTemplate;
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

}