package com.ceo.trading_platform_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TradingPlatformBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingPlatformBackendApplication.class, args);
		System.out.println("Hi, Trading Platform Backend is running!");
	}
	
}
