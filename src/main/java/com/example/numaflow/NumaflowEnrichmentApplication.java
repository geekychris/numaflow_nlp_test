package com.example.numaflow;

import com.example.numaflow.vertex.EnrichmentVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main Spring Boot application for Numaflow text enrichment vertex.
 */
@SpringBootApplication
public class NumaflowEnrichmentApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(NumaflowEnrichmentApplication.class);
    
    public static void main(String[] args) {
        logger.info("Starting Numaflow Enrichment Application");
        
        try {
            ConfigurableApplicationContext context = SpringApplication.run(NumaflowEnrichmentApplication.class, args);
            
            // Get the EnrichmentVertex bean to ensure it's initialized
            EnrichmentVertex vertex = context.getBean(EnrichmentVertex.class);
            
            logger.info("Numaflow Enrichment Application started successfully");
            logger.info("EnrichmentVertex initialized: {}", vertex != null ? "Yes" : "No");
            logger.info("Application is ready to process messages");
            
        } catch (Exception e) {
            logger.error("Failed to start Numaflow Enrichment Application", e);
            System.exit(1);
        }
    }
}
