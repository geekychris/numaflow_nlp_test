package com.example.numaflow.service;

import com.example.numaflow.model.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TestDataGeneratorTest {
    
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private TestDataGenerator testDataGenerator;
    
    @BeforeEach
    void setUp() throws Exception {
        // Mock successful Kafka sends - using lenient to avoid UnnecessaryStubbing warnings
        CompletableFuture<SendResult<String, String>> successFuture = CompletableFuture.completedFuture(null);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(successFuture);
        
        // Mock JSON serialization - using lenient to avoid UnnecessaryStubbing warnings
        lenient().when(objectMapper.writeValueAsString(any(Event.class))).thenReturn("{\"id\":\"test\"}");
    }
    
    @Test
    void generateBatch_withValidCount_shouldGenerateEvents() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 3;
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateBatch(topic, count);
        
        TestDataGenerator.GenerationResult result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(count);
        assertThat(result.getErrorCount()).isEqualTo(0);
        assertThat(result.getTotalCount()).isEqualTo(count);
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getTimestamp()).isNotNull();
        
        // Verify Kafka interactions
        verify(kafkaTemplate, times(count)).send(eq(topic), anyString(), anyString());
        verify(objectMapper, times(count)).writeValueAsString(any(Event.class));
    }
    
    @Test
    void generateBatch_withZeroCount_shouldCompleteImmediately() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 0;
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateBatch(topic, count);
        
        TestDataGenerator.GenerationResult result = future.get(1, TimeUnit.SECONDS);
        
        // Then
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getErrorCount()).isEqualTo(0);
        assertThat(result.getTotalCount()).isEqualTo(0);
        
        // Verify no Kafka interactions
        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(objectMapper);
    }
    
    @Test
    void generateBatch_withKafkaError_shouldTrackErrors() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 2;
        
        CompletableFuture<SendResult<String, String>> errorFuture = new CompletableFuture<>();
        errorFuture.completeExceptionally(new RuntimeException("Kafka error"));
        
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(errorFuture);
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateBatch(topic, count);
        
        TestDataGenerator.GenerationResult result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getErrorCount()).isEqualTo(count);
        assertThat(result.getTotalCount()).isEqualTo(count);
        assertThat(result.hasErrors()).isTrue();
    }
    
    @Test
    void generateBatch_withSerializationError_shouldTrackErrors() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 1;
        
        when(objectMapper.writeValueAsString(any(Event.class)))
            .thenThrow(new RuntimeException("Serialization error"));
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateBatch(topic, count);
        
        TestDataGenerator.GenerationResult result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getErrorCount()).isEqualTo(count);
        assertThat(result.getTotalCount()).isEqualTo(count);
        assertThat(result.hasErrors()).isTrue();
        
        // Verify no Kafka interaction due to serialization failure
        verifyNoInteractions(kafkaTemplate);
    }
    
    @Test
    void generateTestData_withHighRate_shouldGenerateEvents() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 2;
        double ratePerSecond = 100.0; // High rate for quick completion
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateTestData(topic, count, ratePerSecond);
        
        TestDataGenerator.GenerationResult result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(count);
        assertThat(result.getErrorCount()).isEqualTo(0);
        assertThat(result.getTotalCount()).isEqualTo(count);
        
        // Verify Kafka interactions
        verify(kafkaTemplate, times(count)).send(eq(topic), anyString(), anyString());
    }
    
    @Test
    void generateTestData_withLowRate_shouldScheduleEvents() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 2;
        double ratePerSecond = 0.5; // Low rate: 1 event every 2 seconds
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateTestData(topic, count, ratePerSecond);
        
        // Wait a bit for scheduling to start
        Thread.sleep(100);
        
        // Then - should not complete immediately due to low rate
        assertThat(future.isDone()).isFalse();
        
        // Wait for completion
        TestDataGenerator.GenerationResult result = future.get(10, TimeUnit.SECONDS);
        assertThat(result.getSuccessCount()).isEqualTo(count);
        assertThat(result.getErrorCount()).isEqualTo(0);
    }
    
    @Test
    void generateBatch_shouldCreateValidEvents() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 1;
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateBatch(topic, count);
        
        future.get(5, TimeUnit.SECONDS);
        
        // Then
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        Event capturedEvent = eventCaptor.getValue();
        
        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getId()).isNotNull();
        assertThat(capturedEvent.getTimestamp()).isNotNull();
        assertThat(capturedEvent.getTitle()).isNotNull();
        assertThat(capturedEvent.getContent()).isNotNull();
        
        // Verify event has reasonable metadata
        assertThat(capturedEvent.getMetadata()).isNotNull();
        assertThat(capturedEvent.getMetadata().get("eventType")).isIn(
            "Tech News", "Business Update", "Conference Event", 
            "Market Analysis", "Research News", "International Event",
            "Sports News", "Healthcare Innovation"
        );
        assertThat(capturedEvent.getDescription()).isNotNull();
    }
    
    @Test
    void generateBatch_shouldUseCorrectKafkaKeyAndTopic() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 1;
        
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateBatch(topic, count);
        
        future.get(5, TimeUnit.SECONDS);
        
        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());
        
        assertThat(topicCaptor.getValue()).isEqualTo(topic);
        assertThat(keyCaptor.getValue()).isNotNull().isNotEmpty(); // Should be event ID
        assertThat(valueCaptor.getValue()).isEqualTo("{\"id\":\"test\"}"); // Mocked JSON
    }
    
    @Test
    void generateBatch_withMixedResults_shouldTrackBothSuccessAndErrors() throws Exception {
        // Given
        String topic = "test-topic";
        int count = 3;
        
        CompletableFuture<SendResult<String, String>> successFuture = CompletableFuture.completedFuture(null);
        CompletableFuture<SendResult<String, String>> errorFuture = new CompletableFuture<>();
        errorFuture.completeExceptionally(new RuntimeException("Kafka error"));
        
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(successFuture)
            .thenReturn(errorFuture)
            .thenReturn(successFuture);
        
        // When
        CompletableFuture<TestDataGenerator.GenerationResult> future = 
            testDataGenerator.generateBatch(topic, count);
        
        TestDataGenerator.GenerationResult result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getTotalCount()).isEqualTo(count);
        assertThat(result.hasErrors()).isTrue();
    }
    
    @Test
    void generationResult_shouldProvideCorrectProperties() {
        // Given
        int successCount = 5;
        int errorCount = 2;
        
        // When
        TestDataGenerator.GenerationResult result = new TestDataGenerator.GenerationResult(successCount, errorCount);
        
        // Then
        assertThat(result.getSuccessCount()).isEqualTo(successCount);
        assertThat(result.getErrorCount()).isEqualTo(errorCount);
        assertThat(result.getTotalCount()).isEqualTo(successCount + errorCount);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getTimestamp()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }
    
    @Test
    void generationResult_withNoErrors_shouldReportNoErrors() {
        // Given
        int successCount = 5;
        int errorCount = 0;
        
        // When
        TestDataGenerator.GenerationResult result = new TestDataGenerator.GenerationResult(successCount, errorCount);
        
        // Then
        assertThat(result.hasErrors()).isFalse();
    }
}
