package com.alexlondon07.crypto_fetcher_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoFetcherServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoFetcherServiceApplication.class, args);
	}

}
