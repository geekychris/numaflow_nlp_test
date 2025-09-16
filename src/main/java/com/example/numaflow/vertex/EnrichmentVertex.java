package com.example.numaflow.vertex;

import com.example.numaflow.model.EnrichedEvent;
import com.example.numaflow.model.Event;
import com.example.numaflow.service.EventEnrichmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.numaproj.numaflow.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Numaflow Mapper implementation for text enrichment processing.
 * This class implements the Numaflow Mapper interface to process events in a Numaflow pipeline.
 */
@Component
public class EnrichmentVertex extends Mapper {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentVertex.class);
    
    private final EventEnrichmentService enrichmentService;
    private final ObjectMapper objectMapper;
    
    public EnrichmentVertex(EventEnrichmentService enrichmentService, 
                           ObjectMapper objectMapper) {
        this.enrichmentService = enrichmentService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Numaflow Mapper interface implementation.
     * This is the main entry point called by Numaflow to process messages.
     */
    @Override
    public MessageList processMessage(String[] keys, Datum datum) {
        try {
            logger.debug("Processing Numaflow datum with keys: {} at time: {}", 
                String.join(",", keys), datum.getEventTime());
            
            // Parse the incoming message payload as an Event
            String eventJson = new String(datum.getValue());
            Event event = objectMapper.readValue(eventJson, Event.class);
            
            // Set ID if not present
            if (event.getId() == null || event.getId().isEmpty()) {
                event.setId(UUID.randomUUID().toString());
            }
            
            // Process the event through our enrichment service
            EnrichedEvent enrichedEvent = processEventInternal(event);
            
            // Serialize the enriched event
            String enrichedJson = objectMapper.writeValueAsString(enrichedEvent);
            
            // Determine output tags based on processing result
            String[] outputTags = determineOutputTags(enrichedEvent);
            
            // Create output message with tags for routing
            Message outputMessage = new Message(enrichedJson.getBytes(), outputTags);
            
            logger.debug("Successfully processed event ID: {}, output tags: {}", 
                event.getId(), String.join(",", outputTags));
            
            return MessageList.newBuilder().addMessage(outputMessage).build();
            
        } catch (Exception e) {
            logger.error("Error processing message in Numaflow mapper", e);
            
            // Return error message with "error" tag for routing to error sink
            try {
                ErrorResponse errorResponse = new ErrorResponse(
                    UUID.randomUUID().toString(),
                    "ENRICHMENT_ERROR",
                    e.getMessage(),
                    Instant.now()
                );
                
                String errorJson = objectMapper.writeValueAsString(errorResponse);
                Message errorMessage = new Message(errorJson.getBytes(), new String[]{"error"});
                
                return MessageList.newBuilder().addMessage(errorMessage).build();
                
            } catch (Exception jsonError) {
                logger.error("Failed to serialize error response", jsonError);
                
                // Fallback: return simple error message
                String fallbackError = "{\"error\":\"Failed to process message\"}";
                Message fallbackMessage = new Message(fallbackError.getBytes(), new String[]{"error"});
                
                return MessageList.newBuilder().addMessage(fallbackMessage).build();
            }
        }
    }
    
    /**
     * Process JSON event payload and return enriched result as JSON.
     * This method can be used for direct testing.
     */
    public String processEvent(String eventJson) throws Exception {
        logger.debug("Processing event JSON: {}", eventJson);
        
        // Parse the incoming message as an Event
        Event event = objectMapper.readValue(eventJson, Event.class);
        
        // Set ID if not present
        if (event.getId() == null || event.getId().isEmpty()) {
            event.setId(UUID.randomUUID().toString());
        }
        
        // Process the event
        EnrichedEvent enrichedEvent = processEventInternal(event);
        
        // Serialize the enriched event
        String enrichedJson = objectMapper.writeValueAsString(enrichedEvent);
        
        logger.debug("Successfully processed event with ID: {}", event.getId());
        
        return enrichedJson;
    }
    
    /**
     * Determines output tags based on the enriched event status.
     */
    private String[] determineOutputTags(EnrichedEvent enrichedEvent) {
        Object statusObj = enrichedEvent.getEnrichmentMetadata().get("status");
        String status = statusObj != null ? statusObj.toString() : "unknown";
        if ("skipped".equals(status)) {
            return new String[]{"skipped"};
        } else {
            return new String[]{"enriched"};
        }
    }
    
    /**
     * Internal method to process an event (can be called directly for testing).
     */
    public EnrichedEvent processEventInternal(Event event) {
        // Validate that the event can be enriched
        if (!enrichmentService.canEnrichEvent(event)) {
            logger.warn("Event with ID {} cannot be enriched (no text fields)", event.getId());
            
            // Create a minimal enriched event
            EnrichedEvent enrichedEvent = new EnrichedEvent(event);
            enrichedEvent.addEnrichmentMetadata("status", "skipped");
            enrichedEvent.addEnrichmentMetadata("reason", "no_text_fields");
            enrichedEvent.addEnrichmentMetadata("processedAt", Instant.now().toString());
            
            return enrichedEvent;
        }
        
        // Enrich the event
        EnrichedEvent enrichedEvent = enrichmentService.enrichEvent(event);
        enrichedEvent.addEnrichmentMetadata("status", "enriched");
        enrichedEvent.addEnrichmentMetadata("processor", "EnrichmentVertex");
        enrichedEvent.addEnrichmentMetadata("processedAt", Instant.now().toString());
        
        logger.debug("Successfully processed event with ID: {}. " +
            "Segments: {}, Named entities: {}",
            event.getId(),
            enrichedEvent.getAllTextSegments().size(),
            enrichedEvent.getAllNamedEntities().size());
        
        return enrichedEvent;
    }
    
    /**
     * Simple error response class for failed processing
     */
    private static class ErrorResponse {
        public final String id;
        public final String errorType;
        public final String errorMessage;
        public final Instant timestamp;
        
        public ErrorResponse(String id, String errorType, String errorMessage, Instant timestamp) {
            this.id = id;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
        }
    }
}
