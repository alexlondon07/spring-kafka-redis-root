package com.alexlondon07.crypto_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cryptoApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crypto API")
                        .description("REST API for cryptocurrency prices, statistics and history. Provides real-time cryptocurrency data including current prices, price changes, market capitalization, and historical statistics.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Alex London")
                                .email("alex@example.com")
                                .url("https://github.com/alexlondon07"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}