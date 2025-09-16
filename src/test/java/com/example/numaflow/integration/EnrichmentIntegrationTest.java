package com.example.numaflow.integration;

import com.example.numaflow.NumaflowEnrichmentApplication;
import com.example.numaflow.model.EnrichedEvent;
import com.example.numaflow.model.Event;
import com.example.numaflow.service.EventEnrichmentService;
import com.example.numaflow.vertex.EnrichmentVertex;
import com.fasterxml.jackson.databind.ObjectMapper;
// Removed Numaflow SDK dependencies - using direct method calls instead
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the complete enrichment flow.
 */
@SpringBootTest(classes = NumaflowEnrichmentApplication.class)
@ActiveProfiles("test")
class EnrichmentIntegrationTest {
    
    @Autowired
    private EventEnrichmentService eventEnrichmentService;
    
    @Autowired
    private EnrichmentVertex enrichmentVertex;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void completeEnrichmentFlow_shouldWorkEndToEnd() throws Exception {
        // Given
        Event originalEvent = createSampleEvent();
        
        // When - Enrich the event
        EnrichedEvent enrichedEvent = eventEnrichmentService.enrichEvent(originalEvent);
        
        // Then - Verify enrichment results
        assertThat(enrichedEvent).isNotNull();
        assertThat(enrichedEvent.getOriginalEvent()).isEqualTo(originalEvent);
        assertThat(enrichedEvent.getProcessingTimestamp()).isNotNull();
        
        // Verify text fields were processed
        assertThat(enrichedEvent.getEnrichedFields()).containsKeys("title", "description");
        
        // Verify segments were created
        assertThat(enrichedEvent.getAllTextSegments()).isNotEmpty();
        
        // Verify named entities were detected (with fallback implementation)
        assertThat(enrichedEvent.getAllNamedEntities()).isNotEmpty();
        
        // Verify metadata
        assertThat(enrichedEvent.getEnrichmentMetadata()).containsKeys(
            "processedFields", "processingTimeMs", "totalSegments", "totalNamedEntities", "nlpModelsUsed"
        );
        
        // When - Serialize to JSON
        String json = objectMapper.writeValueAsString(enrichedEvent);
        
        // Then - Verify JSON serialization works
        assertThat(json).isNotEmpty();
        assertThat(json).contains("originalEvent");
        assertThat(json).contains("enrichedFields");
        assertThat(json).contains("processingTimestamp");
        
        // When - Deserialize from JSON
        EnrichedEvent deserializedEvent = objectMapper.readValue(json, EnrichedEvent.class);
        
        // Then - Verify deserialization works
        assertThat(deserializedEvent).isNotNull();
        assertThat(deserializedEvent.getOriginalEvent().getId()).isEqualTo(originalEvent.getId());
        assertThat(deserializedEvent.getOriginalEvent().getTitle()).isEqualTo(originalEvent.getTitle());
        assertThat(deserializedEvent.getEnrichedFields()).hasSameSizeAs(enrichedEvent.getEnrichedFields());
    }
    
    @Test
    void enrichmentFlow_withComplexText_shouldHandleProperlySegmentation() {
        // Given
        Event event = new Event(
            "Apple Inc. reports strong quarterly results",
            "The technology giant Apple Inc., based in Cupertino California, announced today " +
            "that its quarterly revenue exceeded expectations. CEO Tim Cook praised the team's efforts. " +
            "The stock price jumped 5% in after-hours trading."
        );
        event.setId(UUID.randomUUID().toString());
        event.setContent("Additional content: Microsoft and Google are also performing well this quarter. " +
                        "Amazon's cloud division continues to grow.");
        
        // When
        EnrichedEvent enrichedEvent = eventEnrichmentService.enrichEvent(event);
        
        // Then
        assertThat(enrichedEvent.getEnrichedFields()).hasSize(3); // title, description, content
        
        // Verify multiple sentences were detected
        assertThat(enrichedEvent.getAllTextSegments()).hasSizeGreaterThan(3);
        
        // Verify named entities were detected (even with fallback)
        assertThat(enrichedEvent.getAllNamedEntities()).isNotEmpty();
        
        // Verify proper company names are detected as entities
        boolean hasCompanyEntities = enrichedEvent.getAllNamedEntities().stream()
            .anyMatch(entity -> entity.getText().matches(".*(Apple|Microsoft|Google|Amazon).*"));
        assertThat(hasCompanyEntities).isTrue();
    }
    
    @Test
    void enrichmentFlow_withMinimalEvent_shouldHandleGracefully() {
        // Given
        Event minimalEvent = new Event("Short", "Brief.");
        
        // When
        EnrichedEvent enrichedEvent = eventEnrichmentService.enrichEvent(minimalEvent);
        
        // Then
        assertThat(enrichedEvent).isNotNull();
        assertThat(enrichedEvent.getEnrichedFields()).hasSize(2);
        assertThat(enrichedEvent.getAllTextSegments()).hasSize(2);
        
        // Each field should have exactly one segment
        assertThat(enrichedEvent.getEnrichedFields().get("title")).hasSize(1);
        assertThat(enrichedEvent.getEnrichedFields().get("description")).hasSize(1);
    }
    
    @Test
    void enrichmentFlow_withMultilingualContent_shouldHandleGracefully() {
        // Given - Mix of English and other characters
        Event event = new Event(
            "International Event: Café & Résumé Discussion",
            "This event will discuss résumés, café culture, and naïve approaches to business."
        );
        
        // When
        EnrichedEvent enrichedEvent = eventEnrichmentService.enrichEvent(event);
        
        // Then - Should handle gracefully without errors
        assertThat(enrichedEvent).isNotNull();
        assertThat(enrichedEvent.getAllTextSegments()).isNotEmpty();
        
        // Verify text is preserved correctly
        String reconstructedTitle = enrichedEvent.getEnrichedFields().get("title").get(0).getText();
        assertThat(reconstructedTitle).contains("Café");
        assertThat(reconstructedTitle).contains("Résumé");
    }
    
    @Test
    void canEnrichEvent_integrationTest() {
        // Given
        Event validEvent = createSampleEvent();
        Event emptyEvent = new Event("", "");
        
        // When/Then
        assertThat(eventEnrichmentService.canEnrichEvent(validEvent)).isTrue();
        assertThat(eventEnrichmentService.canEnrichEvent(emptyEvent)).isFalse();
        assertThat(eventEnrichmentService.canEnrichEvent(null)).isFalse();
    }
    
    @Test
    void numaflowUDF_shouldProcessEventCorrectly() throws Exception {
        // Given - Create a sample event as JSON payload
        Event sampleEvent = createSampleEvent();
        String eventJson = objectMapper.writeValueAsString(sampleEvent);
        
        // When - Process through UDF direct method
        String enrichedJson = enrichmentVertex.processEvent(eventJson);
        
        // Then - Verify UDF processing results
        assertThat(enrichedJson).isNotEmpty();
        
        // Extract the processed message
        EnrichedEvent enrichedEvent = objectMapper.readValue(enrichedJson, EnrichedEvent.class);
        
        // Verify enrichment occurred
        assertThat(enrichedEvent).isNotNull();
        assertThat(enrichedEvent.getOriginalEvent().getId()).isEqualTo(sampleEvent.getId());
        assertThat(enrichedEvent.getAllTextSegments()).isNotEmpty();
        assertThat(enrichedEvent.getAllNamedEntities()).isNotEmpty();
        assertThat(enrichedEvent.getEnrichmentMetadata().get("status")).isEqualTo("enriched");
    }
    
    @Test
    void numaflowUDF_shouldHandleSkippedEvents() throws Exception {
        // Given - Create an event that cannot be enriched (empty fields)
        Event emptyEvent = new Event("", "");
        emptyEvent.setId(UUID.randomUUID().toString());
        String eventJson = objectMapper.writeValueAsString(emptyEvent);
        
        // When - Process through UDF direct method
        String enrichedJson = enrichmentVertex.processEvent(eventJson);
        
        // Then - Verify skipped processing
        assertThat(enrichedJson).isNotEmpty();
        
        EnrichedEvent enrichedEvent = objectMapper.readValue(enrichedJson, EnrichedEvent.class);
        
        // Verify it was marked as skipped
        assertThat(enrichedEvent.getEnrichmentMetadata().get("status")).isEqualTo("skipped");
        assertThat(enrichedEvent.getEnrichmentMetadata().get("reason")).isEqualTo("no_text_fields");
    }
    
    private Event createSampleEvent() {
        Event event = new Event(
            "John Doe speaks at Microsoft conference in Seattle",
            "Software engineer John Doe will present his research on artificial intelligence " +
            "at the Microsoft developer conference held in Seattle, Washington. The event is " +
            "sponsored by Google and Amazon Web Services."
        );
        event.setId(UUID.randomUUID().toString());
        event.setTimestamp(Instant.now());
        return event;
    }
}
