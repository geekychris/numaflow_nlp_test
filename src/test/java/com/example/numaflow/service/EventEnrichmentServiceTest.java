package com.example.numaflow.service;

import com.example.numaflow.model.EnrichedEvent;
import com.example.numaflow.model.Event;
import com.example.numaflow.model.NamedEntity;
import com.example.numaflow.model.TextSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventEnrichmentService.
 */
@ExtendWith(MockitoExtension.class)
class EventEnrichmentServiceTest {
    
    private EventEnrichmentService eventEnrichmentService;
    
    @Mock
    private NlpEnrichmentService nlpService;
    
    @BeforeEach
    void setUp() {
        eventEnrichmentService = new EventEnrichmentService(nlpService);
    }
    
    @Test
    void enrichEvent_withValidEvent_shouldReturnEnrichedEvent() {
        // Given
        Event event = new Event("Test Title", "Test Description");
        event.setId("test-id");
        
        TextSegment titleSegment = new TextSegment("Test Title", 0, 10, 1);
        titleSegment.addNamedEntity(new NamedEntity("Test", "UNKNOWN", 0, 4, 0.5));
        
        TextSegment descSegment = new TextSegment("Test Description", 0, 16, 1);
        descSegment.addNamedEntity(new NamedEntity("Description", "UNKNOWN", 5, 16, 0.5));
        
        when(nlpService.enrichText("Test Title")).thenReturn(List.of(titleSegment));
        when(nlpService.enrichText("Test Description")).thenReturn(List.of(descSegment));
        
        // When
        EnrichedEvent enrichedEvent = eventEnrichmentService.enrichEvent(event);
        
        // Then
        assertThat(enrichedEvent).isNotNull();
        assertThat(enrichedEvent.getOriginalEvent()).isEqualTo(event);
        assertThat(enrichedEvent.getEnrichedFields()).hasSize(2);
        assertThat(enrichedEvent.getEnrichedFields()).containsKeys("title", "description");
        
        // Check metadata
        assertThat(enrichedEvent.getEnrichmentMetadata()).containsKeys(
            "processedFields", "processingTimeMs", "totalSegments", "totalNamedEntities", "nlpModelsUsed"
        );
        
        // Check processed fields
        List<String> processedFields = (List<String>) enrichedEvent.getEnrichmentMetadata().get("processedFields");
        assertThat(processedFields).containsExactly("title", "description");
        
        // Check totals
        assertThat(enrichedEvent.getAllTextSegments()).hasSize(2);
        assertThat(enrichedEvent.getAllNamedEntities()).hasSize(2);
    }
    
    @Test
    void enrichEvent_withNullEvent_shouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> eventEnrichmentService.enrichEvent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Event cannot be null");
    }
    
    @Test
    void enrichEvent_withEventWithAllFields_shouldProcessAllFields() {
        // Given
        Event event = new Event("Title", "Description");
        event.setContent("Content text");
        event.setSummary("Summary text");
        event.setId("test-id");
        
        TextSegment mockSegment = new TextSegment("Mock", 0, 4, 1);
        when(nlpService.enrichText(anyString())).thenReturn(List.of(mockSegment));
        
        // When
        EnrichedEvent enrichedEvent = eventEnrichmentService.enrichEvent(event);
        
        // Then
        assertThat(enrichedEvent.getEnrichedFields()).hasSize(4);
        assertThat(enrichedEvent.getEnrichedFields()).containsKeys("title", "description", "content", "summary");
        
        List<String> processedFields = (List<String>) enrichedEvent.getEnrichmentMetadata().get("processedFields");
        assertThat(processedFields).containsExactlyInAnyOrder("title", "description", "content", "summary");
    }
    
    @Test
    void enrichEvent_withEmptyTextFields_shouldOnlyProcessNonEmptyFields() {
        // Given
        Event event = new Event("Valid Title", "");
        event.setContent("   "); // Whitespace only
        event.setSummary("Valid Summary");
        
        TextSegment mockSegment = new TextSegment("Mock", 0, 4, 1);
        when(nlpService.enrichText("Valid Title")).thenReturn(List.of(mockSegment));
        when(nlpService.enrichText("Valid Summary")).thenReturn(List.of(mockSegment));
        
        // When
        EnrichedEvent enrichedEvent = eventEnrichmentService.enrichEvent(event);
        
        // Then
        assertThat(enrichedEvent.getEnrichedFields()).hasSize(2);
        assertThat(enrichedEvent.getEnrichedFields()).containsKeys("title", "summary");
        
        List<String> processedFields = (List<String>) enrichedEvent.getEnrichmentMetadata().get("processedFields");
        assertThat(processedFields).containsExactlyInAnyOrder("title", "summary");
    }
    
    @Test
    void enrichEvents_withValidEventList_shouldReturnEnrichedEvents() {
        // Given
        Event event1 = new Event("Title 1", "Description 1");
        Event event2 = new Event("Title 2", "Description 2");
        List<Event> events = Arrays.asList(event1, event2);
        
        TextSegment mockSegment = new TextSegment("Mock", 0, 4, 1);
        when(nlpService.enrichText(anyString())).thenReturn(List.of(mockSegment));
        
        // When
        List<EnrichedEvent> enrichedEvents = eventEnrichmentService.enrichEvents(events);
        
        // Then
        assertThat(enrichedEvents).hasSize(2);
        assertThat(enrichedEvents.get(0).getOriginalEvent()).isEqualTo(event1);
        assertThat(enrichedEvents.get(1).getOriginalEvent()).isEqualTo(event2);
    }
    
    @Test
    void enrichEvents_withEmptyList_shouldReturnEmptyList() {
        // When
        List<EnrichedEvent> enrichedEvents = eventEnrichmentService.enrichEvents(List.of());
        
        // Then
        assertThat(enrichedEvents).isEmpty();
    }
    
    @Test
    void enrichEvents_withNullList_shouldReturnEmptyList() {
        // When
        List<EnrichedEvent> enrichedEvents = eventEnrichmentService.enrichEvents(null);
        
        // Then
        assertThat(enrichedEvents).isEmpty();
    }
    
    @Test
    void canEnrichEvent_withValidEvent_shouldReturnTrue() {
        // Given
        Event event = new Event("Title", "Description");
        
        // When
        boolean canEnrich = eventEnrichmentService.canEnrichEvent(event);
        
        // Then
        assertThat(canEnrich).isTrue();
    }
    
    @Test
    void canEnrichEvent_withEmptyTextFields_shouldReturnFalse() {
        // Given
        Event event = new Event("", "");
        event.setContent("");
        event.setSummary(null);
        
        // When
        boolean canEnrich = eventEnrichmentService.canEnrichEvent(event);
        
        // Then
        assertThat(canEnrich).isFalse();
    }
    
    @Test
    void canEnrichEvent_withNullEvent_shouldReturnFalse() {
        // When
        boolean canEnrich = eventEnrichmentService.canEnrichEvent(null);
        
        // Then
        assertThat(canEnrich).isFalse();
    }
    
    @Test
    void canEnrichEvent_withOnlyContentField_shouldReturnTrue() {
        // Given
        Event event = new Event("", "");
        event.setContent("Some content");
        
        // When
        boolean canEnrich = eventEnrichmentService.canEnrichEvent(event);
        
        // Then
        assertThat(canEnrich).isTrue();
    }
    
    @Test
    void enrichEvent_shouldSetProcessingTimestamp() {
        // Given
        Event event = new Event("Title", "Description");
        Instant beforeProcessing = Instant.now();
        
        TextSegment mockSegment = new TextSegment("Mock", 0, 4, 1);
        when(nlpService.enrichText(anyString())).thenReturn(List.of(mockSegment));
        
        // When
        EnrichedEvent enrichedEvent = eventEnrichmentService.enrichEvent(event);
        
        // Then
        Instant afterProcessing = Instant.now();
        assertThat(enrichedEvent.getProcessingTimestamp()).isBetween(beforeProcessing, afterProcessing);
        
        // Check that processing time is recorded in metadata
        Long processingTimeMs = (Long) enrichedEvent.getEnrichmentMetadata().get("processingTimeMs");
        assertThat(processingTimeMs).isNotNull();
        assertThat(processingTimeMs).isGreaterThanOrEqualTo(0);
    }
}
