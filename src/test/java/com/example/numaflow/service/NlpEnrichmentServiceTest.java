package com.example.numaflow.service;

import com.example.numaflow.model.NamedEntity;
import com.example.numaflow.model.TextSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NlpEnrichmentService.
 */
@ExtendWith(MockitoExtension.class)
class NlpEnrichmentServiceTest {
    
    private NlpEnrichmentService nlpService;
    
    @Mock
    private Resource sentenceModelResource;
    
    @Mock
    private Resource tokenizerModelResource;
    
    @Mock
    private Resource personModelResource;
    
    @Mock
    private Resource locationModelResource;
    
    @Mock
    private Resource organizationModelResource;
    
    @BeforeEach
    void setUp() {
        nlpService = new NlpEnrichmentService();
        
        // Set mock resources
        ReflectionTestUtils.setField(nlpService, "sentenceModelResource", sentenceModelResource);
        ReflectionTestUtils.setField(nlpService, "tokenizerModelResource", tokenizerModelResource);
        ReflectionTestUtils.setField(nlpService, "personModelResource", personModelResource);
        ReflectionTestUtils.setField(nlpService, "locationModelResource", locationModelResource);
        ReflectionTestUtils.setField(nlpService, "organizationModelResource", organizationModelResource);
        
        // Mock that resources don't exist (so we use fallback implementations)
        when(sentenceModelResource.exists()).thenReturn(false);
        when(tokenizerModelResource.exists()).thenReturn(false);
        when(personModelResource.exists()).thenReturn(false);
        when(locationModelResource.exists()).thenReturn(false);
        when(organizationModelResource.exists()).thenReturn(false);
        
        // Initialize the service
        nlpService.initializeModels();
    }
    
    @Test
    void enrichText_withValidText_shouldReturnSegments() {
        // Given
        String text = "John Doe works at Microsoft in Seattle. He loves programming.";
        
        // When
        List<TextSegment> segments = nlpService.enrichText(text);
        
        // Then
        assertThat(segments).isNotEmpty();
        assertThat(segments).hasSizeGreaterThan(0);
        
        // Check that segments contain the text
        String combinedText = segments.stream()
            .map(TextSegment::getText)
            .reduce("", (a, b) -> a + " " + b)
            .trim();
        
        assertThat(combinedText).contains("John Doe");
        assertThat(combinedText).contains("Microsoft");
        assertThat(combinedText).contains("Seattle");
    }
    
    @Test
    void enrichText_withEmptyText_shouldReturnEmptyList() {
        // Given
        String text = "";
        
        // When
        List<TextSegment> segments = nlpService.enrichText(text);
        
        // Then
        assertThat(segments).isEmpty();
    }
    
    @Test
    void enrichText_withNullText_shouldReturnEmptyList() {
        // Given
        String text = null;
        
        // When
        List<TextSegment> segments = nlpService.enrichText(text);
        
        // Then
        assertThat(segments).isEmpty();
    }
    
    @Test
    void enrichText_withMultipleSentences_shouldReturnMultipleSegments() {
        // Given
        String text = "First sentence here. Second sentence follows! Third one ends with question?";
        
        // When
        List<TextSegment> segments = nlpService.enrichText(text);
        
        // Then
        assertThat(segments).hasSizeGreaterThanOrEqualTo(3);
        
        // Check segment numbers
        for (int i = 0; i < segments.size(); i++) {
            assertThat(segments.get(i).getSegmentNumber()).isEqualTo(i + 1);
        }
    }
    
    @Test
    void enrichText_withCapitalizedWords_shouldDetectNamedEntities() {
        // Given
        String text = "Apple Inc. is located in California.";
        
        // When
        List<TextSegment> segments = nlpService.enrichText(text);
        
        // Then
        assertThat(segments).isNotEmpty();
        
        // Check that some named entities are detected (using fallback implementation)
        boolean hasNamedEntities = segments.stream()
            .anyMatch(segment -> !segment.getNamedEntities().isEmpty());
        
        // With fallback implementation, capitalized words should be detected
        assertThat(hasNamedEntities).isTrue();
    }
    
    @Test
    void enrichText_fallbackNamedEntityRecognition_shouldDetectCapitalizedWords() {
        // Given
        String text = "John works at Microsoft.";
        
        // When
        List<TextSegment> segments = nlpService.enrichText(text);
        
        // Then
        assertThat(segments).hasSize(1);
        
        TextSegment segment = segments.get(0);
        List<NamedEntity> entities = segment.getNamedEntities();
        
        // Should detect "John" and "Microsoft" as entities (fallback implementation)
        assertThat(entities).hasSize(2);
        
        NamedEntity johnEntity = entities.stream()
            .filter(entity -> entity.getText().equals("John"))
            .findFirst()
            .orElse(null);
        
        assertThat(johnEntity).isNotNull();
        assertThat(johnEntity.getType()).isEqualTo("UNKNOWN");
        assertThat(johnEntity.getConfidence()).isEqualTo(0.5);
        
        NamedEntity microsoftEntity = entities.stream()
            .filter(entity -> entity.getText().equals("Microsoft"))
            .findFirst()
            .orElse(null);
        
        assertThat(microsoftEntity).isNotNull();
        assertThat(microsoftEntity.getType()).isEqualTo("UNKNOWN");
    }
    
    @Test
    void enrichText_withComplexText_shouldProvideCorrectIndices() {
        // Given
        String text = "The quick brown fox jumps over the lazy dog. This is a test sentence.";
        
        // When
        List<TextSegment> segments = nlpService.enrichText(text);
        
        // Then
        assertThat(segments).isNotEmpty();
        
        for (TextSegment segment : segments) {
            // Verify that start and end indices are valid
            assertThat(segment.getStartIndex()).isGreaterThanOrEqualTo(0);
            assertThat(segment.getEndIndex()).isGreaterThan(segment.getStartIndex());
            assertThat(segment.getEndIndex()).isLessThanOrEqualTo(text.length());
            
            // Verify that the text matches the indices
            String extractedText = text.substring(segment.getStartIndex(), segment.getEndIndex()).trim();
            assertThat(segment.getText()).isEqualTo(extractedText);
        }
    }
}
