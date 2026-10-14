package com.ceo.trading_platform_backend;

import java.time.Instant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.ceo.trading_platform_backend.models.Client;

@SpringBootApplication
public class TradingPlatformBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingPlatformBackendApplication.class, args);
		Client me = new Client(
			"Drake Maye", 
			"drakemaye@not.real", 
			"goat", 
			Instant.now()
		);
		System.out.println(
			String.format("User: %s; email %s", me.getFullName(),  me.getEmail())
		);
		System.out.println("Hi, Trading Platform Backend is running!");
	}

}
