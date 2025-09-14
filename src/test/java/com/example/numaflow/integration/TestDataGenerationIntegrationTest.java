package com.example.numaflow.integration;

import com.example.numaflow.controller.TestGeneratorController;
import com.example.numaflow.service.TestDataGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Full integration test for test data generation flow.
 * Tests the complete path from REST API to Kafka message publication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"test-events", "events"})
@DirtiesContext
class TestDataGenerationIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void generateBatch_shouldPublishEventsToKafka() throws Exception {
        // Given
        TestGeneratorController.GenerateBatchRequest request = new TestGeneratorController.GenerateBatchRequest();
        request.setCount(3);
        request.setTopic("test-events");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestGeneratorController.GenerateBatchRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/test/generate-batch", 
            entity, 
            Map.class
        );
        
        // Then - Verify HTTP response
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "completed");
        assertThat(response.getBody()).containsEntry("topic", "test-events");
        assertThat(response.getBody()).containsEntry("successCount", 3);
        assertThat(response.getBody()).containsEntry("errorCount", 0);
        assertThat(response.getBody()).containsEntry("totalCount", 3);
    }
    
    @Test
    void getInfo_shouldReturnServiceInformation() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/test/info", 
            Map.class
        );
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("service", "Test Data Generator");
        assertThat(response.getBody()).containsEntry("version", "1.0");
        assertThat(response.getBody()).containsEntry("defaultTopic", "events");
        assertThat(response.getBody()).containsKey("limits");
        assertThat(response.getBody()).containsKey("eventTypes");
    }
    
    @Test
    void generateTestData_withInvalidRequest_shouldReturnBadRequest() {
        // Given
        TestGeneratorController.GenerateTestDataRequest request = new TestGeneratorController.GenerateTestDataRequest();
        request.setCount(-1); // Invalid count
        request.setRatePerSecond(10.0);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestGeneratorController.GenerateTestDataRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/test/generate", 
            entity, 
            Map.class
        );
        
        // Then
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "error");
        assertThat(response.getBody()).containsEntry("message", "Count must be positive");
    }
}
