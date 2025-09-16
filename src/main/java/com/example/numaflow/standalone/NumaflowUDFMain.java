package com.example.numaflow.standalone;

import com.example.numaflow.model.EnrichedEvent;
import com.example.numaflow.model.Event;
import com.example.numaflow.service.EventEnrichmentService;
import com.example.numaflow.service.NlpEnrichmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.numaproj.numaflow.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Standalone Numaflow UDF implementation that doesn't depend on Spring Boot.
 * This ensures compatibility with the Numaflow Java SDK.
 */
public class NumaflowUDFMain {
    
    private static final Logger logger = LoggerFactory.getLogger(NumaflowUDFMain.class);
    
    public static void main(String[] args) {
        logger.info("Starting standalone Numaflow UDF for text enrichment");
        
        try {
            // Initialize dependencies manually (without Spring)
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            
            NlpEnrichmentService nlpService = new NlpEnrichmentService();
            EventEnrichmentService enrichmentService = new EventEnrichmentService(nlpService);
            
            // Create the mapper
            EnrichmentMapper mapper = new EnrichmentMapper(enrichmentService, objectMapper);
            
            // Start Numaflow UDF server
            Server server = new Server(mapper);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Numaflow UDF server");
                try {
                    server.stop();
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));
            
            logger.info("Starting Numaflow UDF server...");
            server.start();
            logger.info("Numaflow UDF server is running");
            
        } catch (Exception e) {
            logger.error("Failed to start Numaflow UDF", e);
            System.exit(1);
        }
    }
    
    /**
     * Numaflow Mapper implementation for text enrichment.
     */
    static class EnrichmentMapper extends Mapper {
        
        private final EventEnrichmentService enrichmentService;
        private final ObjectMapper objectMapper;
        private final Logger logger = LoggerFactory.getLogger(EnrichmentMapper.class);
        
        public EnrichmentMapper(EventEnrichmentService enrichmentService, ObjectMapper objectMapper) {
            this.enrichmentService = enrichmentService;
            this.objectMapper = objectMapper;
        }
        
        @Override
        public MessageList processMessage(String[] keys, Datum datum) {
            try {
                logger.debug("Processing message with keys: {}", String.join(",", keys));
                
                // Parse input JSON
                String eventJson = new String(datum.getValue());
                Event event = objectMapper.readValue(eventJson, Event.class);
                
                // Ensure event has ID
                if (event.getId() == null || event.getId().isEmpty()) {
                    event.setId(UUID.randomUUID().toString());
                }
                
                // Process the event
                EnrichedEvent enrichedEvent;
                String[] tags;
                
                if (!enrichmentService.canEnrichEvent(event)) {
                    // Create skipped event
                    enrichedEvent = new EnrichedEvent(event);
                    enrichedEvent.addEnrichmentMetadata("status", "skipped");
                    enrichedEvent.addEnrichmentMetadata("reason", "no_text_fields");
                    enrichedEvent.addEnrichmentMetadata("processedAt", Instant.now().toString());
                    tags = new String[]{"skipped"};
                    
                    logger.debug("Event {} skipped - no text fields", event.getId());
                } else {
                    // Enrich the event
                    enrichedEvent = enrichmentService.enrichEvent(event);
                    enrichedEvent.addEnrichmentMetadata("status", "enriched");
                    enrichedEvent.addEnrichmentMetadata("processor", "NumaflowUDF");
                    enrichedEvent.addEnrichmentMetadata("processedAt", Instant.now().toString());
                    tags = new String[]{"enriched"};
                    
                    logger.debug("Event {} enriched with {} segments and {} entities", 
                        event.getId(),
                        enrichedEvent.getAllTextSegments().size(),
                        enrichedEvent.getAllNamedEntities().size());
                }
                
                // Serialize result
                String resultJson = objectMapper.writeValueAsString(enrichedEvent);
                
                // Create output message
                Message message = new Message(resultJson.getBytes(), tags);
                
                return MessageList.newBuilder().addMessage(message).build();
                
            } catch (Exception e) {
                logger.error("Error processing message", e);
                
                // Return error message
                try {
                    ErrorInfo error = new ErrorInfo(
                        UUID.randomUUID().toString(),
                        "PROCESSING_ERROR", 
                        e.getMessage(),
                        Instant.now()
                    );
                    
                    String errorJson = objectMapper.writeValueAsString(error);
                    Message errorMessage = new Message(errorJson.getBytes(), new String[]{"error"});
                    
                    return MessageList.newBuilder().addMessage(errorMessage).build();
                    
                } catch (Exception jsonError) {
                    logger.error("Failed to serialize error", jsonError);
                    
                    // Fallback error message
                    String fallback = "{\"error\":\"Processing failed\",\"timestamp\":\"" + Instant.now() + "\"}";
                    Message fallbackMessage = new Message(fallback.getBytes(), new String[]{"error"});
                    
                    return MessageList.newBuilder().addMessage(fallbackMessage).build();
                }
            }
        }
    }
    
    /**
     * Error information for failed processing.
     */
    static class ErrorInfo {
        public final String id;
        public final String errorType;
        public final String message;
        public final Instant timestamp;
        
        public ErrorInfo(String id, String errorType, String message, Instant timestamp) {
            this.id = id;
            this.errorType = errorType;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
