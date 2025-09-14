package com.example.numaflow.service;

import com.example.numaflow.model.NamedEntity;
import com.example.numaflow.model.TextSegment;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for enriching text with sentence segmentation and named entity recognition using OpenNLP.
 */
@Service
public class NlpEnrichmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(NlpEnrichmentService.class);
    
    @Value("classpath:models/en-sent.bin")
    private Resource sentenceModelResource;
    
    @Value("classpath:models/en-token.bin")
    private Resource tokenizerModelResource;
    
    @Value("classpath:models/en-ner-person.bin")
    private Resource personModelResource;
    
    @Value("classpath:models/en-ner-location.bin")
    private Resource locationModelResource;
    
    @Value("classpath:models/en-ner-organization.bin")
    private Resource organizationModelResource;
    
    private SentenceDetectorME sentenceDetector;
    private TokenizerME tokenizer;
    private final Map<String, NameFinderME> nameFinders = new HashMap<>();
    
    @PostConstruct
    public void initializeModels() {
        logger.info("Initializing OpenNLP models...");
        
        try {
            // Initialize sentence detector
            if (sentenceModelResource.exists()) {
                try (InputStream modelIn = sentenceModelResource.getInputStream()) {
                    SentenceModel sentenceModel = new SentenceModel(modelIn);
                    sentenceDetector = new SentenceDetectorME(sentenceModel);
                    logger.info("Sentence detection model loaded successfully");
                }
            } else {
                logger.warn("Sentence model not found, using fallback implementation");
                // We'll create a fallback implementation
            }
            
            // Initialize tokenizer
            if (tokenizerModelResource.exists()) {
                try (InputStream modelIn = tokenizerModelResource.getInputStream()) {
                    TokenizerModel tokenizerModel = new TokenizerModel(modelIn);
                    tokenizer = new TokenizerME(tokenizerModel);
                    logger.info("Tokenizer model loaded successfully");
                }
            } else {
                logger.warn("Tokenizer model not found, using fallback implementation");
            }
            
            // Initialize NER models
            initializeNerModel(personModelResource, "PERSON");
            initializeNerModel(locationModelResource, "LOCATION");
            initializeNerModel(organizationModelResource, "ORGANIZATION");
            
            logger.info("OpenNLP models initialization completed");
            
        } catch (IOException e) {
            logger.error("Failed to initialize OpenNLP models", e);
            // Initialize fallback implementations
            initializeFallbackModels();
        }
    }
    
    private void initializeNerModel(Resource modelResource, String entityType) {
        try {
            if (modelResource.exists()) {
                try (InputStream modelIn = modelResource.getInputStream()) {
                    TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
                    nameFinders.put(entityType, new NameFinderME(model));
                    logger.info("NER model for {} loaded successfully", entityType);
                }
            } else {
                logger.warn("NER model for {} not found", entityType);
            }
        } catch (IOException e) {
            logger.error("Failed to load NER model for {}", entityType, e);
        }
    }
    
    private void initializeFallbackModels() {
        logger.info("Initializing fallback NLP implementations");
        // Fallback models will be simple rule-based implementations
    }
    
    /**
     * Enriches text by performing sentence segmentation and named entity recognition.
     * 
     * @param text the input text to enrich
     * @return list of text segments with NER annotations
     */
    public List<TextSegment> enrichText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.debug("Enriching text: {}", text.substring(0, Math.min(100, text.length())));
        
        List<TextSegment> segments = segmentText(text);
        
        // Add named entities to each segment
        for (TextSegment segment : segments) {
            List<NamedEntity> entities = extractNamedEntities(segment.getText(), segment.getStartIndex());
            segment.setNamedEntities(entities);
        }
        
        logger.debug("Text enrichment completed. Found {} segments", segments.size());
        return segments;
    }
    
    /**
     * Segments text into sentences.
     * 
     * @param text the input text
     * @return list of text segments (sentences)
     */
    private List<TextSegment> segmentText(String text) {
        List<TextSegment> segments = new ArrayList<>();
        
        if (sentenceDetector != null) {
            // Use OpenNLP sentence detector
            Span[] sentenceSpans = sentenceDetector.sentPosDetect(text);
            
            for (int i = 0; i < sentenceSpans.length; i++) {
                Span span = sentenceSpans[i];
                String sentenceText = text.substring(span.getStart(), span.getEnd()).trim();
                
                if (!sentenceText.isEmpty()) {
                    TextSegment segment = new TextSegment(
                        sentenceText,
                        span.getStart(),
                        span.getEnd(),
                        i + 1
                    );
                    segments.add(segment);
                }
            }
        } else {
            // Fallback: simple sentence splitting
            segments = fallbackSentenceSegmentation(text);
        }
        
        return segments;
    }
    
    /**
     * Fallback sentence segmentation using simple rules.
     */
    private List<TextSegment> fallbackSentenceSegmentation(String text) {
        List<TextSegment> segments = new ArrayList<>();
        
        // Simple sentence splitting by periods, exclamation marks, and question marks
        String[] sentences = text.split("(?<=[.!?])\\s+");
        int currentIndex = 0;
        
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                int startIndex = text.indexOf(sentence, currentIndex);
                int endIndex = startIndex + sentence.length();
                
                TextSegment segment = new TextSegment(sentence, startIndex, endIndex, i + 1);
                segments.add(segment);
                
                currentIndex = endIndex;
            }
        }
        
        return segments;
    }
    
    /**
     * Extracts named entities from text.
     * 
     * @param text the input text
     * @param textStartIndex the start index of this text within the larger document
     * @return list of named entities
     */
    private List<NamedEntity> extractNamedEntities(String text, int textStartIndex) {
        List<NamedEntity> entities = new ArrayList<>();
        
        if (tokenizer != null && !nameFinders.isEmpty()) {
            // Tokenize the text
            String[] tokens = tokenizer.tokenize(text);
            Span[] tokenSpans = tokenizer.tokenizePos(text);
            
            // Apply each NER model
            for (Map.Entry<String, NameFinderME> entry : nameFinders.entrySet()) {
                String entityType = entry.getKey();
                NameFinderME finder = entry.getValue();
                
                Span[] entitySpans = finder.find(tokens);
                
                for (Span entitySpan : entitySpans) {
                    // Get the text for this entity
                    StringBuilder entityText = new StringBuilder();
                    int entityStartIndex = tokenSpans[entitySpan.getStart()].getStart();
                    int entityEndIndex = tokenSpans[entitySpan.getEnd() - 1].getEnd();
                    
                    for (int i = entitySpan.getStart(); i < entitySpan.getEnd(); i++) {
                        if (entityText.length() > 0) {
                            entityText.append(" ");
                        }
                        entityText.append(tokens[i]);
                    }
                    
                    NamedEntity entity = new NamedEntity(
                        entityText.toString(),
                        entityType,
                        textStartIndex + entityStartIndex,
                        textStartIndex + entityEndIndex,
                        entitySpan.getProb()
                    );
                    
                    entities.add(entity);
                }
                
                // Clear adaptive data
                finder.clearAdaptiveData();
            }
        } else {
            // Fallback NER implementation
            entities = fallbackNamedEntityRecognition(text, textStartIndex);
        }
        
        return entities;
    }
    
    /**
     * Fallback named entity recognition using simple patterns.
     */
    private List<NamedEntity> fallbackNamedEntityRecognition(String text, int textStartIndex) {
        List<NamedEntity> entities = new ArrayList<>();
        
        // Simple pattern-based NER (this is very basic and should be improved)
        // Look for capitalized words as potential entities
        String[] words = text.split("\\s+");
        int currentIndex = 0;
        
        for (String word : words) {
            // Find the word in the original text to get accurate positions
            int wordStart = text.indexOf(word, currentIndex);
            if (wordStart != -1) {
                int wordEnd = wordStart + word.length();
                
                // Simple heuristic: if word is capitalized, might be an entity
                // Clean the word first (remove punctuation)
                String cleanWord = word.replaceAll("[.!?,:;]", "");
                if (Character.isUpperCase(cleanWord.charAt(0)) && cleanWord.length() > 1) {
                    // This is a very simple heuristic - in practice you'd want better rules
                    // Use the cleaned word as the entity text but keep original positions
                    NamedEntity entity = new NamedEntity(
                        cleanWord,
                        "UNKNOWN", // Generic type for fallback
                        textStartIndex + wordStart,
                        textStartIndex + wordStart + cleanWord.length(),
                        0.5 // Low confidence for fallback
                    );
                    entities.add(entity);
                }
                
                currentIndex = wordEnd;
            }
        }
        
        return entities;
    }
}
