package com.example.numaflow.controller;

import com.example.numaflow.service.TestDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for test data generation.
 * Provides endpoints to trigger test event generation with various parameters.
 */
@RestController
@RequestMapping("/api/test")
public class TestGeneratorController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestGeneratorController.class);
    
    @Autowired
    private TestDataGenerator testDataGenerator;
    
    /**
     * Generate test events with rate limiting.
     * 
     * @param request Test generation request parameters
     * @return Response with generation status
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTestData(@RequestBody GenerateTestDataRequest request) {
        logger.info("Received test data generation request: {} events at {} events/sec to topic '{}'", 
                   request.getCount(), request.getRatePerSecond(), request.getTopic());
        
        // Validate request
        if (request.getCount() <= 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Count must be positive"));
        }
        if (request.getRatePerSecond() <= 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Rate must be positive"));
        }
        if (request.getCount() > 10000) {
            return ResponseEntity.badRequest().body(createErrorResponse("Count cannot exceed 10,000"));
        }
        if (request.getRatePerSecond() > 1000) {
            return ResponseEntity.badRequest().body(createErrorResponse("Rate cannot exceed 1,000 events/sec"));
        }
        
        String topic = request.getTopic() != null ? request.getTopic() : "events";
        
        try {
            CompletableFuture<TestDataGenerator.GenerationResult> futureResult = 
                testDataGenerator.generateTestData(topic, request.getCount(), request.getRatePerSecond());
            
            // For async generation, return immediately with acceptance
            Map<String, Object> response = new HashMap<>();
            response.put("status", "accepted");
            response.put("message", String.format("Generating %d events at %.2f events/sec", 
                                                 request.getCount(), request.getRatePerSecond()));
            response.put("topic", topic);
            response.put("estimatedDurationSeconds", request.getCount() / request.getRatePerSecond());
            
            // If rate is high enough, wait for completion (for smaller batches)
            if (request.getRatePerSecond() >= 10 && request.getCount() <= 100) {
                TestDataGenerator.GenerationResult result = futureResult.get();
                response.put("status", "completed");
                response.put("successCount", result.getSuccessCount());
                response.put("errorCount", result.getErrorCount());
                response.put("totalCount", result.getTotalCount());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting test data generation", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to start generation: " + e.getMessage()));
        }
    }
    
    /**
     * Generate a batch of test events immediately (no rate limiting).
     * 
     * @param request Batch generation request
     * @return Response with generation results
     */
    @PostMapping("/generate-batch")
    public ResponseEntity<Map<String, Object>> generateBatch(@RequestBody GenerateBatchRequest request) {
        logger.info("Received batch generation request: {} events to topic '{}'", 
                   request.getCount(), request.getTopic());
        
        // Validate request
        if (request.getCount() <= 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Count must be positive"));
        }
        if (request.getCount() > 1000) {
            return ResponseEntity.badRequest().body(createErrorResponse("Batch count cannot exceed 1,000"));
        }
        
        String topic = request.getTopic() != null ? request.getTopic() : "events";
        
        try {
            CompletableFuture<TestDataGenerator.GenerationResult> futureResult = 
                testDataGenerator.generateBatch(topic, request.getCount());
            
            // Wait for batch completion
            TestDataGenerator.GenerationResult result = futureResult.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "completed");
            response.put("topic", topic);
            response.put("successCount", result.getSuccessCount());
            response.put("errorCount", result.getErrorCount());
            response.put("totalCount", result.getTotalCount());
            response.put("timestamp", result.getTimestamp());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating batch data", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to generate batch: " + e.getMessage()));
        }
    }
    
    /**
     * Get information about available test generation capabilities.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Test Data Generator");
        info.put("version", "1.0");
        info.put("limits", Map.of(
            "maxCount", 10000,
            "maxRatePerSecond", 1000,
            "maxBatchCount", 1000
        ));
        info.put("defaultTopic", "events");
        info.put("eventTypes", new String[]{
            "Tech News", "Business Update", "Conference Event", 
            "Market Analysis", "Research News", "International Event", 
            "Sports News", "Healthcare Innovation"
        });
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Generate a few sample events for testing (convenience method).
     */
    @PostMapping("/sample")
    public ResponseEntity<Map<String, Object>> generateSample(@RequestParam(defaultValue = "events") String topic) {
        logger.info("Generating sample events to topic '{}'", topic);
        
        try {
            CompletableFuture<TestDataGenerator.GenerationResult> futureResult = 
                testDataGenerator.generateBatch(topic, 5);
            
            TestDataGenerator.GenerationResult result = futureResult.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "completed");
            response.put("message", "Sample events generated");
            response.put("topic", topic);
            response.put("successCount", result.getSuccessCount());
            response.put("errorCount", result.getErrorCount());
            response.put("totalCount", result.getTotalCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating sample data", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to generate samples: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return error;
    }
    
    /**
     * Request DTO for test data generation with rate limiting.
     */
    public static class GenerateTestDataRequest {
        private int count;
        private double ratePerSecond;
        private String topic;
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public double getRatePerSecond() { return ratePerSecond; }
        public void setRatePerSecond(double ratePerSecond) { this.ratePerSecond = ratePerSecond; }
        
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
    }
    
    /**
     * Request DTO for batch generation.
     */
    public static class GenerateBatchRequest {
        private int count;
        private String topic;
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
    }
}
