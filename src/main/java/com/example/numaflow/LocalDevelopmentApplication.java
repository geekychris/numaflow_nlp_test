package com.example.numaflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Local development main application that runs as a standard Spring Boot app
 * without starting the Numaflow UDF gRPC server. This enables easy debugging
 * in IntelliJ while the Numaflow infrastructure runs in Kubernetes.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LocalDevelopmentApplication {

    private static final Logger logger = LoggerFactory.getLogger(LocalDevelopmentApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Numaflow Enrichment Application in LOCAL DEVELOPMENT mode");
        
        // Set local profile if not already set
        System.setProperty("spring.profiles.active", "local");
        
        try {
            ConfigurableApplicationContext context = SpringApplication.run(LocalDevelopmentApplication.class, args);
            Environment env = context.getEnvironment();
            
            String serverPort = env.getProperty("server.port", "8080");
            String appName = env.getProperty("spring.application.name", "numaflow-enrichment-app");
            
            logger.info("=".repeat(50));
            logger.info("🚀 {} started successfully!", appName);
            logger.info("🌐 REST API: http://localhost:{}", serverPort);
            logger.info("📊 Health: http://localhost:{}/actuator/health", serverPort);
            logger.info("📈 Metrics: http://localhost:{}/actuator/metrics", serverPort);
            logger.info("🧪 Test API: http://localhost:{}/api/test/sample", serverPort);
            logger.info("🔧 Environment: {}", env.getProperty("spring.profiles.active"));
            logger.info("=".repeat(50));
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Local Development Application...");
                context.close();
                logger.info("Local Development Application shut down gracefully");
            }));
            
        } catch (Exception e) {
            logger.error("Failed to start Local Development Application", e);
            System.exit(1);
        }
    }
}