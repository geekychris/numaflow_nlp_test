package com.example.numaflow.controller;

import com.example.numaflow.model.EnrichedEvent;
import com.example.numaflow.model.Event;
import com.example.numaflow.vertex.EnrichmentVertex;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.numaproj.numaflow.mapper.Datum;
import io.numaproj.numaflow.mapper.Message;
import io.numaproj.numaflow.mapper.MessageList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Debug controller that provides REST endpoints to simulate Numaflow message processing.
 * This allows you to test the exact same code that would run in the Numaflow UDF
 * but through REST calls that can be easily debugged in IntelliJ.
 * 
 * Only enabled when app.mode=local-development
 */
@RestController
@RequestMapping("/api/debug")
@ConditionalOnProperty(name = "app.mode", havingValue = "local-development")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    private final EnrichmentVertex enrichmentVertex;
    private final ObjectMapper objectMapper;

    public DebugController(EnrichmentVertex enrichmentVertex, ObjectMapper objectMapper) {
        this.enrichmentVertex = enrichmentVertex;
        this.objectMapper = objectMapper;
    }

    /**
     * Simulate Numaflow message processing via REST.
     * This calls the exact same processMessage method that Numaflow would call.
     */
    @PostMapping("/numaflow/process")
    public ResponseEntity<?> simulateNumaflowProcessing(@RequestBody Event event,
                                                       @RequestParam(defaultValue = "event") String[] keys) {
        try {
            logger.info("🔧 DEBUG: Simulating Numaflow processing for event: {}", event.getId());

            // Convert event to JSON (simulating what Numaflow would send)
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Create a mock Datum (simulating Numaflow's input)
            Datum mockDatum = new MockDatum(eventJson.getBytes(), Instant.now());
            
            // Call the actual Numaflow processMessage method
            MessageList result = enrichmentVertex.processMessage(keys, mockDatum);
            
            // Convert the result to a more readable format
            List<ProcessedMessage> processedMessages = new ArrayList<>();
            for (Message message : result.getMessages()) {
                ProcessedMessage pm = new ProcessedMessage(
                    new String(message.getValue()),
                    Arrays.asList(message.getTags())
                );
                processedMessages.add(pm);
            }
            
            DebugResult debugResult = new DebugResult(
                event.getId(),
                "SUCCESS",
                processedMessages,
                Instant.now()
            );
            
            logger.info("✅ DEBUG: Successfully processed event {} with {} output messages", 
                event.getId(), processedMessages.size());
            
            return ResponseEntity.ok(debugResult);
            
        } catch (Exception e) {
            logger.error("❌ DEBUG: Error processing event: {}", event.getId(), e);
            
            DebugResult errorResult = new DebugResult(
                event.getId(),
                "ERROR: " + e.getMessage(),
                Collections.emptyList(),
                Instant.now()
            );
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * Direct enrichment endpoint that bypasses Numaflow simulation.
     * This calls the enrichment logic directly for simpler debugging.
     */
    @PostMapping("/direct/enrich")
    public ResponseEntity<?> directEnrichment(@RequestBody Event event) {
        try {
            logger.info("🔧 DEBUG: Direct enrichment for event: {}", event.getId());

            // Call enrichment directly (bypass Numaflow layer)
            EnrichedEvent result = enrichmentVertex.processEventInternal(event);
            
            logger.info("✅ DEBUG: Successfully enriched event {} directly", event.getId());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("❌ DEBUG: Error in direct enrichment for event: {}", event.getId(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get debug information about the current enrichment vertex configuration.
     */
    @GetMapping("/info")
    public ResponseEntity<?> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Debug Controller");
        info.put("description", "REST endpoints for debugging Numaflow enrichment processing");
        info.put("mode", "local-development");
        info.put("timestamp", Instant.now());
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("numaflow-simulation", "POST /api/debug/numaflow/process");
        endpoints.put("direct-enrichment", "POST /api/debug/direct/enrich");
        endpoints.put("debug-info", "GET /api/debug/info");
        info.put("endpoints", endpoints);
        
        Map<String, String> sampleUsage = new HashMap<>();
        sampleUsage.put("simulate-numaflow", "curl -X POST http://localhost:8080/api/debug/numaflow/process -H 'Content-Type: application/json' -d '{\"id\":\"test-1\",\"title\":\"Test event\"}'");
        sampleUsage.put("direct-enrich", "curl -X POST http://localhost:8080/api/debug/direct/enrich -H 'Content-Type: application/json' -d '{\"id\":\"test-1\",\"title\":\"Test event\"}'");
        info.put("sample-usage", sampleUsage);
        
        return ResponseEntity.ok(info);
    }

    /**
     * Simple mock implementation of Numaflow's Datum interface for testing
     */
    private static class MockDatum implements Datum {
        private final byte[] value;
        private final Instant eventTime;

        public MockDatum(byte[] value, Instant eventTime) {
            this.value = value;
            this.eventTime = eventTime;
        }

        @Override
        public byte[] getValue() {
            return value;
        }

        @Override
        public Instant getEventTime() {
            return eventTime;
        }

        @Override
        public Instant getWatermark() {
            return eventTime;
        }

        @Override
        public Map<String, String> getHeaders() {
            return Map.of();
        }
    }

    /**
     * Result structure for debug responses
     */
    public static class DebugResult {
        public final String eventId;
        public final String status;
        public final List<ProcessedMessage> messages;
        public final Instant timestamp;

        public DebugResult(String eventId, String status, List<ProcessedMessage> messages, Instant timestamp) {
            this.eventId = eventId;
            this.status = status;
            this.messages = messages;
            this.timestamp = timestamp;
        }
    }

    /**
     * Processed message structure for debug responses
     */
    public static class ProcessedMessage {
        public final String payload;
        public final List<String> tags;

        public ProcessedMessage(String payload, List<String> tags) {
            this.payload = payload;
            this.tags = tags;
        }
    }
}