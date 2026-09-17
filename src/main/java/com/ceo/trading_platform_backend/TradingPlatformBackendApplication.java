package com.ceo.trading_platform_backend;

import java.time.OffsetDateTime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.ceo.trading_platform_backend.models.User;

@SpringBootApplication
public class TradingPlatformBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingPlatformBackendApplication.class, args);
		User me = new User(
			"Drake Maye", 
			"drakemaye@not.real", 
			"goat", OffsetDateTime.now(), 
			"Quarterback"
		);
		System.out.println(
			String.format("User: %s; email %s", me.getFullName(),  me.getEmail())
		);
		System.out.println("Hi, Trading Platform Backend is running!");
	}

}
