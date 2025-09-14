package com.example.numaflow.service;

import com.example.numaflow.model.EnrichedEvent;
import com.example.numaflow.model.Event;
import com.example.numaflow.model.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for enriching events with NLP processing results.
 */
@Service
public class EventEnrichmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventEnrichmentService.class);
    
    private final NlpEnrichmentService nlpService;
    
    public EventEnrichmentService(NlpEnrichmentService nlpService) {
        this.nlpService = nlpService;
    }
    
    /**
     * Enriches an event by processing all text fields with NLP.
     * 
     * @param event the original event to enrich
     * @return enriched event with NLP processing results
     */
    public EnrichedEvent enrichEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        logger.debug("Enriching event with ID: {}", event.getId());
        Instant startTime = Instant.now();
        
        EnrichedEvent enrichedEvent = new EnrichedEvent(event);
        
        // Process each text field in the event
        List<String> processedFields = new ArrayList<>();
        
        // Process title
        if (event.getTitle() != null && !event.getTitle().trim().isEmpty()) {
            List<TextSegment> titleSegments = nlpService.enrichText(event.getTitle());
            enrichedEvent.addEnrichedField("title", titleSegments);
            processedFields.add("title");
        }
        
        // Process description
        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            List<TextSegment> descriptionSegments = nlpService.enrichText(event.getDescription());
            enrichedEvent.addEnrichedField("description", descriptionSegments);
            processedFields.add("description");
        }
        
        // Process content if present
        if (event.getContent() != null && !event.getContent().trim().isEmpty()) {
            List<TextSegment> contentSegments = nlpService.enrichText(event.getContent());
            enrichedEvent.addEnrichedField("content", contentSegments);
            processedFields.add("content");
        }
        
        // Process summary if present
        if (event.getSummary() != null && !event.getSummary().trim().isEmpty()) {
            List<TextSegment> summarySegments = nlpService.enrichText(event.getSummary());
            enrichedEvent.addEnrichedField("summary", summarySegments);
            processedFields.add("summary");
        }
        
        // Add enrichment metadata
        Instant endTime = Instant.now();
        long processingTimeMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        
        enrichedEvent.addEnrichmentMetadata("processedFields", processedFields);
        enrichedEvent.addEnrichmentMetadata("processingTimeMs", processingTimeMs);
        enrichedEvent.addEnrichmentMetadata("totalSegments", enrichedEvent.getAllTextSegments().size());
        enrichedEvent.addEnrichmentMetadata("totalNamedEntities", enrichedEvent.getAllNamedEntities().size());
        enrichedEvent.addEnrichmentMetadata("nlpModelsUsed", "OpenNLP");
        
        logger.debug("Event enrichment completed for ID: {} in {}ms. " +
                "Found {} segments and {} named entities.",
                event.getId(), processingTimeMs,
                enrichedEvent.getAllTextSegments().size(),
                enrichedEvent.getAllNamedEntities().size());
        
        return enrichedEvent;
    }
    
    /**
     * Enriches a batch of events.
     * 
     * @param events the list of events to enrich
     * @return list of enriched events
     */
    public List<EnrichedEvent> enrichEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.info("Enriching batch of {} events", events.size());
        
        List<EnrichedEvent> enrichedEvents = new ArrayList<>();
        for (Event event : events) {
            try {
                EnrichedEvent enrichedEvent = enrichEvent(event);
                enrichedEvents.add(enrichedEvent);
            } catch (Exception e) {
                logger.error("Failed to enrich event with ID: {}", event.getId(), e);
                // Optionally, you could add a partially enriched event or skip this event
                // For now, we'll continue processing other events
            }
        }
        
        logger.info("Batch enrichment completed. Successfully enriched {}/{} events",
                enrichedEvents.size(), events.size());
        
        return enrichedEvents;
    }
    
    /**
     * Validates if an event can be enriched.
     * 
     * @param event the event to validate
     * @return true if the event has text fields that can be processed
     */
    public boolean canEnrichEvent(Event event) {
        if (event == null) {
            return false;
        }
        
        return (event.getTitle() != null && !event.getTitle().trim().isEmpty()) ||
               (event.getDescription() != null && !event.getDescription().trim().isEmpty()) ||
               (event.getContent() != null && !event.getContent().trim().isEmpty()) ||
               (event.getSummary() != null && !event.getSummary().trim().isEmpty());
    }
}
