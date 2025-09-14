package com.example.numaflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an enriched event containing the original event data plus NLP processing results.
 */
public class EnrichedEvent {
    
    @JsonProperty("originalEvent")
    private Event originalEvent;
    
    @JsonProperty("enrichedFields")
    private Map<String, List<TextSegment>> enrichedFields;
    
    @JsonProperty("processingTimestamp")
    private Instant processingTimestamp;
    
    @JsonProperty("enrichmentMetadata")
    private Map<String, Object> enrichmentMetadata;
    
    public EnrichedEvent() {
        this.enrichedFields = new HashMap<>();
        this.enrichmentMetadata = new HashMap<>();
        this.processingTimestamp = Instant.now();
    }
    
    public EnrichedEvent(Event originalEvent) {
        this();
        this.originalEvent = originalEvent;
    }
    
    /**
     * Add enriched text segments for a specific field from the original event.
     * 
     * @param fieldName the name of the field (e.g., "title", "description")
     * @param segments the text segments with NLP enrichment
     */
    public void addEnrichedField(String fieldName, List<TextSegment> segments) {
        if (segments != null && !segments.isEmpty()) {
            this.enrichedFields.put(fieldName, new ArrayList<>(segments));
        }
    }
    
    /**
     * Get all named entities across all enriched fields.
     * 
     * @return consolidated list of all named entities
     */
    public List<NamedEntity> getAllNamedEntities() {
        List<NamedEntity> allEntities = new ArrayList<>();
        for (List<TextSegment> segments : enrichedFields.values()) {
            for (TextSegment segment : segments) {
                allEntities.addAll(segment.getNamedEntities());
            }
        }
        return allEntities;
    }
    
    /**
     * Get all text segments across all enriched fields.
     * 
     * @return consolidated list of all text segments
     */
    public List<TextSegment> getAllTextSegments() {
        List<TextSegment> allSegments = new ArrayList<>();
        for (List<TextSegment> segments : enrichedFields.values()) {
            allSegments.addAll(segments);
        }
        return allSegments;
    }
    
    // Getters and Setters
    public Event getOriginalEvent() {
        return originalEvent;
    }
    
    public void setOriginalEvent(Event originalEvent) {
        this.originalEvent = originalEvent;
    }
    
    public Map<String, List<TextSegment>> getEnrichedFields() {
        return enrichedFields;
    }
    
    public void setEnrichedFields(Map<String, List<TextSegment>> enrichedFields) {
        this.enrichedFields = enrichedFields != null ? enrichedFields : new HashMap<>();
    }
    
    public Instant getProcessingTimestamp() {
        return processingTimestamp;
    }
    
    public void setProcessingTimestamp(Instant processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }
    
    public Map<String, Object> getEnrichmentMetadata() {
        return enrichmentMetadata;
    }
    
    public void setEnrichmentMetadata(Map<String, Object> enrichmentMetadata) {
        this.enrichmentMetadata = enrichmentMetadata != null ? enrichmentMetadata : new HashMap<>();
    }
    
    public void addEnrichmentMetadata(String key, Object value) {
        if (this.enrichmentMetadata == null) {
            this.enrichmentMetadata = new HashMap<>();
        }
        this.enrichmentMetadata.put(key, value);
    }
    
    @Override
    public String toString() {
        return "EnrichedEvent{" +
                "originalEvent=" + originalEvent +
                ", enrichedFields=" + enrichedFields +
                ", processingTimestamp=" + processingTimestamp +
                ", enrichmentMetadata=" + enrichmentMetadata +
                '}';
    }
}
