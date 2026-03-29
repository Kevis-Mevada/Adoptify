package com.adoptify.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // Added import
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync // Enabled async
public class AdoptifyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdoptifyBackendApplication.class, args);
	}

}
