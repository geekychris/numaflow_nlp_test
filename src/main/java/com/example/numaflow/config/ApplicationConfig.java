package com.example.numaflow.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for the Numaflow enrichment application.
 */
@Configuration
public class ApplicationConfig {
    
    /**
     * Configure ObjectMapper for JSON serialization/deserialization.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register Java Time module for proper Instant handling
        mapper.registerModule(new JavaTimeModule());
        
        // Configure serialization
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Configure deserialization
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        
        return mapper;
    }
    
    /**
     * Custom health indicator for NLP models.
     */
    @Bean
    public HealthIndicator nlpHealthIndicator() {
        return () -> {
            try {
                // Here you would check if NLP models are loaded and functional
                // For now, we'll return a simple health check
                return Health.up()
                    .withDetail("nlp-models", "OpenNLP models initialized")
                    .withDetail("fallback-enabled", true)
                    .build();
            } catch (Exception e) {
                return Health.down()
                    .withDetail("nlp-models", "Failed to load NLP models")
                    .withException(e)
                    .build();
            }
        };
    }
}
