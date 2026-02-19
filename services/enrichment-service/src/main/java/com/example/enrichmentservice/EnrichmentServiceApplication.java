package com.example.enrichmentservice;

import com.example.enrichmentservice.config.GroqProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GroqProperties.class)
public class EnrichmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnrichmentServiceApplication.class, args);
	}

}
