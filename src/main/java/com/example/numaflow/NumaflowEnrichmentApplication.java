package com.example.numaflow;

import com.example.numaflow.vertex.EnrichmentVertex;
import io.numaproj.numaflow.mapper.Mapper;
import io.numaproj.numaflow.mapper.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main Spring Boot application for Numaflow text enrichment UDF.
 * Starts both Spring Boot context and Numaflow gRPC server.
 */
@SpringBootApplication
public class NumaflowEnrichmentApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(NumaflowEnrichmentApplication.class);
    
    public static void main(String[] args) {
        logger.info("Starting Numaflow Enrichment UDF Application");
        
        try {
            // Start Spring Boot context
            ConfigurableApplicationContext context = SpringApplication.run(NumaflowEnrichmentApplication.class, args);
            
            // Get the mapper implementation from Spring context
            EnrichmentVertex enrichmentVertex = context.getBean(EnrichmentVertex.class);
            
            logger.info("Spring Boot context started successfully");
            logger.info("EnrichmentVertex Mapper initialized: {}", enrichmentVertex != null ? "Yes" : "No");
            
            // Start Numaflow gRPC UDF server
            logger.info("Starting Numaflow UDF gRPC server...");
            
            Server numaflowServer = new Server((Mapper) enrichmentVertex);
            
            // Add shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Numaflow UDF server...");
                try {
                    numaflowServer.stop();
                    context.close();
                    logger.info("Application shut down gracefully");
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));
            
            // Start the Numaflow server (this will block)
            numaflowServer.start();
            
            logger.info("Numaflow UDF gRPC server started and ready to process messages");
            
        } catch (Exception e) {
            logger.error("Failed to start Numaflow Enrichment UDF Application", e);
            System.exit(1);
        }
    }
}
