package com.alexlondon07.crypto_fetcher_service.config;

import com.alexlondon07.crypto_fetcher_service.utils.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(Constants.COINGECKO_BASE_URL)
                .build();
    }

}
