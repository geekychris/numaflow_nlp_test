package com.example.numaflow.vertex;

import com.example.numaflow.model.EnrichedEvent;
import com.example.numaflow.model.Event;
import com.example.numaflow.service.EventEnrichmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka-based event enrichment processor (can be adapted for Numaflow later).
 */
@Component
public class EnrichmentVertex {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentVertex.class);
    
    private final EventEnrichmentService enrichmentService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public EnrichmentVertex(EventEnrichmentService enrichmentService, 
                           ObjectMapper objectMapper,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.enrichmentService = enrichmentService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Kafka listener for incoming events to be enriched.
     */
    @KafkaListener(topics = "events", groupId = "enrichment-service")
    public void processEvent(@Payload String eventJson,
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                           @Header(KafkaHeaders.OFFSET) long offset) {
        
        try {
            logger.debug("Processing event from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset);
            
            // Parse the incoming message as an Event
            Event event = objectMapper.readValue(eventJson, Event.class);
            
            // Set ID if not present
            if (event.getId() == null || event.getId().isEmpty()) {
                event.setId(UUID.randomUUID().toString());
            }
            
            // Process the event
            EnrichedEvent enrichedEvent = processEventInternal(event);
            
            // Send to output topic
            String enrichedJson = objectMapper.writeValueAsString(enrichedEvent);
            
            // Determine output topic based on processing result
            final String outputTopic = "skipped".equals(enrichedEvent.getEnrichmentMetadata().get("status")) 
                ? "skipped-events" 
                : "enriched-events";
            
            final String eventId = event.getId();
            
            kafkaTemplate.send(outputTopic, eventId, enrichedJson)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send enriched event to topic {}", outputTopic, ex);
                    } else {
                        logger.debug("Successfully sent enriched event {} to topic {}", 
                            eventId, outputTopic);
                    }
                });
            
        } catch (Exception e) {
            logger.error("Failed to process event from topic: {}", topic, e);
            
            // Send error to error topic
            try {
                ErrorResponse errorResponse = new ErrorResponse(
                    UUID.randomUUID().toString(),
                    "ENRICHMENT_ERROR",
                    e.getMessage(),
                    Instant.now()
                );
                
                String errorJson = objectMapper.writeValueAsString(errorResponse);
                kafkaTemplate.send("enrichment-errors", errorResponse.id, errorJson);
                
            } catch (Exception serializationError) {
                logger.error("Failed to serialize error response", serializationError);
            }
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
