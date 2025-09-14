package com.example.numaflow.controller;

import com.example.numaflow.model.EnrichedEvent;
import com.example.numaflow.model.Event;
import com.example.numaflow.service.EventEnrichmentService;
import com.example.numaflow.vertex.EnrichmentVertex;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for testing the enrichment functionality.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    private final EventEnrichmentService enrichmentService;
    private final EnrichmentVertex enrichmentVertex;
    private final ObjectMapper objectMapper;
    
    public TestController(EventEnrichmentService enrichmentService, 
                         EnrichmentVertex enrichmentVertex,
                         ObjectMapper objectMapper) {
        this.enrichmentService = enrichmentService;
        this.enrichmentVertex = enrichmentVertex;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Test the enrichment service directly.
     */
    @PostMapping("/enrich")
    public ResponseEntity<EnrichedEvent> testEnrichment(@RequestBody Event event) {
        try {
            logger.info("Testing enrichment for event: {}", event.getId());
            
            if (event.getId() == null || event.getId().isEmpty()) {
                event.setId(UUID.randomUUID().toString());
            }
            
            EnrichedEvent enrichedEvent = enrichmentService.enrichEvent(event);
            
            logger.info("Enrichment completed. Segments: {}, Named entities: {}", 
                enrichedEvent.getAllTextSegments().size(),
                enrichedEvent.getAllNamedEntities().size());
            
            return ResponseEntity.ok(enrichedEvent);
            
        } catch (Exception e) {
            logger.error("Error during enrichment test", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Test the enrichment vertex directly.
     */
    @PostMapping("/vertex")
    public ResponseEntity<EnrichedEvent> testVertex(@RequestBody Event event) {
        try {
            logger.info("Testing vertex for event: {}", event.getId());
            
            if (event.getId() == null || event.getId().isEmpty()) {
                event.setId(UUID.randomUUID().toString());
            }
            
            // Process through vertex internal method
            EnrichedEvent result = enrichmentVertex.processEventInternal(event);
            
            logger.info("Vertex processing completed. Status: {}", 
                result.getEnrichmentMetadata().get("status"));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error during vertex test", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create a sample event for testing.
     */
    @GetMapping("/sample-event")
    public ResponseEntity<Event> getSampleEvent() {
        Event event = new Event(
            "Apple Inc. reports strong quarterly results",
            "The technology giant Apple Inc., based in Cupertino California, announced today " +
            "that its quarterly revenue exceeded expectations. CEO Tim Cook praised the team's efforts."
        );
        event.setId(UUID.randomUUID().toString());
        event.setTimestamp(Instant.now());
        
        return ResponseEntity.ok(event);
    }
    
    /**
     * Health check for the enrichment components.
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            // Test with a simple event
            Event testEvent = new Event("Test title", "Test description");
            boolean canEnrich = enrichmentService.canEnrichEvent(testEvent);
            
            return ResponseEntity.ok(String.format(
                "Enrichment service status: %s, Vertex initialized: %s",
                canEnrich ? "OK" : "ERROR",
                enrichmentVertex != null ? "Yes" : "No"
            ));
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.internalServerError()
                .body("Health check failed: " + e.getMessage());
        }
    }
    
}
