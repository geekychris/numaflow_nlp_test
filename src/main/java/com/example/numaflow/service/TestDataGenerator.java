package com.example.numaflow.service;

import com.example.numaflow.model.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Service for generating test data for Numaflow UDF testing.
 * Supports creating sample events for processing.
 */
@Service
public class TestDataGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Test data templates
    private final List<EventTemplate> eventTemplates = Arrays.asList(
        new EventTemplate(
            "Tech News",
            "Apple Inc. announces breakthrough in artificial intelligence technology",
            "The technology giant Apple Inc., based in Cupertino California, announced today that its quarterly results exceeded expectations. The company's focus on machine learning and artificial intelligence continues to drive innovation."
        ),
        new EventTemplate(
            "Business Update",
            "Microsoft and Google partnership drives cloud innovation",
            "Software giants Microsoft and Google announced a strategic partnership today. The collaboration focuses on cloud computing services and artificial intelligence development. Amazon's cloud division is expected to respond with new initiatives."
        ),
        new EventTemplate(
            "Conference Event",
            "John Doe speaks at Microsoft conference in Seattle",
            "Software engineer John Doe will present his research on artificial intelligence at the Microsoft developer conference in Seattle. The presentation covers machine learning algorithms and their applications in enterprise software."
        ),
        new EventTemplate(
            "Market Analysis",
            "Tesla stock surges after quarterly earnings report",
            "Electric vehicle manufacturer Tesla reported strong quarterly earnings today. CEO Elon Musk highlighted the company's progress in autonomous driving technology and battery innovation. Wall Street analysts predict continued growth."
        ),
        new EventTemplate(
            "Research News",
            "Stanford University publishes breakthrough study",
            "Researchers at Stanford University in California published groundbreaking research on quantum computing. The study, led by Dr. Sarah Johnson, demonstrates significant advances in quantum error correction."
        ),
        new EventTemplate(
            "International Event",
            "Global climate summit begins in Paris",
            "World leaders gather in Paris, France for the annual climate summit. The European Union and United States are expected to announce new environmental initiatives. China and India will also present their sustainability plans."
        ),
        new EventTemplate(
            "Sports News",
            "NBA Finals reach exciting climax",
            "The Golden State Warriors face the Boston Celtics in Game 7 of the NBA Finals. Star players Stephen Curry and Jayson Tatum lead their respective teams. The game takes place at TD Garden in Boston, Massachusetts."
        ),
        new EventTemplate(
            "Healthcare Innovation",
            "FDA approves revolutionary cancer treatment",
            "The Food and Drug Administration approved a new cancer treatment developed by Pfizer Inc. Clinical trials conducted at Johns Hopkins Hospital in Baltimore, Maryland showed promising results for patients with advanced melanoma."
        )
    );
    
    /**
     * Generates a list of test events.
     * 
     * @param count Number of events to generate
     * @return List of generated events
     */
    public List<Event> generateTestEvents(int count) {
        logger.info("Generating {} test events", count);
        
        List<Event> events = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            events.add(generateRandomEvent(random));
        }
        
        logger.debug("Generated {} test events successfully", events.size());
        return events;
    }
    
    /**
     * Generates a single random test event.
     * 
     * @return Random test event
     */
    public Event generateSingleEvent() {
        return generateRandomEvent(new Random());
    }
    
    /**
     * Generates a random event based on templates.
     */
    private Event generateRandomEvent(Random random) {
        EventTemplate template = eventTemplates.get(random.nextInt(eventTemplates.size()));
        
        Event event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setTimestamp(java.time.Instant.now());
        event.setTitle(template.title);
        event.setContent(template.content);
        
        // Add some variation to the content
        if (random.nextBoolean()) {
            event.setDescription("Additional details: " + generateRandomSentence(random));
        } else {
            event.setDescription(template.category + ": " + template.content.substring(0, Math.min(100, template.content.length())));
        }
        
        if (random.nextDouble() < 0.3) { // 30% chance of summary
            event.setSummary(generateRandomSentence(random));
        }
        
        // Add metadata with event category
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("eventType", template.category);
        metadata.put("generated", true);
        metadata.put("generator", "TestDataGenerator");
        event.setMetadata(metadata);
        
        return event;
    }
    
    /**
     * Generates a random sentence for variety.
     */
    private String generateRandomSentence(Random random) {
        String[] subjects = {"The company", "Researchers", "The team", "Industry experts", "Analysts", "The organization"};
        String[] verbs = {"announced", "discovered", "developed", "revealed", "demonstrated", "achieved"};
        String[] objects = {"significant progress", "breakthrough results", "innovative solutions", "new capabilities", "advanced technology", "important findings"};
        String[] contexts = {"in this field", "for the industry", "with global impact", "through collaboration", "using cutting-edge methods", "ahead of schedule"};
        
        return subjects[random.nextInt(subjects.length)] + " " +
               verbs[random.nextInt(verbs.length)] + " " +
               objects[random.nextInt(objects.length)] + " " +
               contexts[random.nextInt(contexts.length)] + ".";
    }
    
    /**
     * Result of test data generation.
     */
    public static class GenerationResult {
        private final int successCount;
        private final int errorCount;
        private final LocalDateTime timestamp;
        
        public GenerationResult(int successCount, int errorCount) {
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.timestamp = LocalDateTime.now();
        }
        
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public int getTotalCount() { return successCount + errorCount; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean hasErrors() { return errorCount > 0; }
    }
    
    /**
     * Template for generating test events.
     */
    private static class EventTemplate {
        final String category;
        final String title;
        final String content;
        
        EventTemplate(String category, String title, String content) {
            this.category = category;
            this.title = title;
            this.content = content;
        }
    }
}
