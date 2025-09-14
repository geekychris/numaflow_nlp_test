package com.example.numaflow.controller;

import com.example.numaflow.service.TestDataGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestGeneratorController.class)
class TestGeneratorControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TestDataGenerator testDataGenerator;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void generateTestData_withValidRequest_shouldReturnAccepted() throws Exception {
        // Given
        TestGeneratorController.GenerateTestDataRequest request = new TestGeneratorController.GenerateTestDataRequest();
        request.setCount(10);
        request.setRatePerSecond(2.0);
        request.setTopic("test-topic");
        
        TestDataGenerator.GenerationResult result = new TestDataGenerator.GenerationResult(10, 0);
        when(testDataGenerator.generateTestData(anyString(), anyInt(), anyDouble()))
            .thenReturn(CompletableFuture.completedFuture(result));
        
        // When & Then
        mockMvc.perform(post("/api/test/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.topic").value("test-topic"))
                .andExpect(jsonPath("$.estimatedDurationSeconds").value(5.0));
    }
    
    @Test
    void generateBatch_withValidRequest_shouldReturnSuccess() throws Exception {
        // Given
        TestGeneratorController.GenerateBatchRequest request = new TestGeneratorController.GenerateBatchRequest();
        request.setCount(20);
        request.setTopic("batch-topic");
        
        TestDataGenerator.GenerationResult result = new TestDataGenerator.GenerationResult(20, 0);
        when(testDataGenerator.generateBatch(anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(result));
        
        // When & Then
        mockMvc.perform(post("/api/test/generate-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.topic").value("batch-topic"))
                .andExpect(jsonPath("$.successCount").value(20))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.totalCount").value(20))
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void generateTestData_withNegativeCount_shouldReturnBadRequest() throws Exception {
        // Given
        TestGeneratorController.GenerateTestDataRequest request = new TestGeneratorController.GenerateTestDataRequest();
        request.setCount(-1);
        request.setRatePerSecond(2.0);
        
        // When & Then
        mockMvc.perform(post("/api/test/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Count must be positive"));
    }
    
    @Test
    void getInfo_shouldReturnServiceInformation() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/test/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("Test Data Generator"))
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.limits.maxCount").value(10000))
                .andExpect(jsonPath("$.limits.maxRatePerSecond").value(1000))
                .andExpect(jsonPath("$.limits.maxBatchCount").value(1000))
                .andExpect(jsonPath("$.defaultTopic").value("events"))
                .andExpect(jsonPath("$.eventTypes").isArray())
                .andExpect(jsonPath("$.eventTypes").value(hasSize(8)));
    }
    
    @Test
    void generateSample_withDefaultTopic_shouldGenerateSamples() throws Exception {
        // Given
        TestDataGenerator.GenerationResult result = new TestDataGenerator.GenerationResult(5, 0);
        when(testDataGenerator.generateBatch(eq("events"), eq(5)))
            .thenReturn(CompletableFuture.completedFuture(result));
        
        // When & Then
        mockMvc.perform(post("/api/test/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.message").value("Sample events generated"))
                .andExpect(jsonPath("$.topic").value("events"))
                .andExpect(jsonPath("$.successCount").value(5))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.totalCount").value(5));
    }
}
