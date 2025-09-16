package com.example.numaflow.controller;

import com.example.numaflow.vertex.EnrichmentVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller that exposes the enrichment functionality as HTTP endpoints for Numaflow.
 * Numaflow can call these endpoints to process events instead of using Kafka directly.
 */
@RestController
@RequestMapping("/udf")
public class UdfController {

    private static final Logger logger = LoggerFactory.getLogger(UdfController.class);

    private final EnrichmentVertex enrichmentVertex;

    public UdfController(EnrichmentVertex enrichmentVertex) {
        this.enrichmentVertex = enrichmentVertex;
    }

    /**
     * Main UDF endpoint for processing events.
     * Accepts JSON event payload and returns enriched JSON result.
     */
    @PostMapping(value = "/enrich", 
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> enrichEvent(@RequestBody String eventJson) {
        try {
            logger.debug("Received UDF request for event enrichment");
            
            String enrichedJson = enrichmentVertex.processEvent(eventJson);
            
            logger.debug("Successfully processed UDF enrichment request");
            return ResponseEntity.ok(enrichedJson);
            
        } catch (Exception e) {
            logger.error("Failed to process UDF enrichment request", e);
            
            // Return error as JSON response
            String errorResponse = String.format(
                "{\"error\":\"ENRICHMENT_ERROR\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                e.getMessage().replace("\"", "\\\""),
                java.time.Instant.now().toString()
            );
            
            return ResponseEntity.status(500)
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(errorResponse);
        }
    }

    /**
     * Health check endpoint for Numaflow.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"healthy\",\"service\":\"text-enrichment-udf\"}");
    }
}
