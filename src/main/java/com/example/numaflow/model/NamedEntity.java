package com.example.numaflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a named entity extracted from text using NLP processing.
 */
public class NamedEntity {
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("type")
    private String type;  // PERSON, ORGANIZATION, LOCATION, etc.
    
    @JsonProperty("startIndex")
    private int startIndex;
    
    @JsonProperty("endIndex")
    private int endIndex;
    
    @JsonProperty("confidence")
    private double confidence;
    
    public NamedEntity() {}
    
    public NamedEntity(String text, String type, int startIndex, int endIndex) {
        this.text = text;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.confidence = 1.0; // Default confidence
    }
    
    public NamedEntity(String text, String type, int startIndex, int endIndex, double confidence) {
        this.text = text;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.confidence = confidence;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getStartIndex() {
        return startIndex;
    }
    
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }
    
    public int getEndIndex() {
        return endIndex;
    }
    
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    @Override
    public String toString() {
        return "NamedEntity{" +
                "text='" + text + '\'' +
                ", type='" + type + '\'' +
                ", startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                ", confidence=" + confidence +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        NamedEntity that = (NamedEntity) obj;
        return startIndex == that.startIndex &&
               endIndex == that.endIndex &&
               Double.compare(that.confidence, confidence) == 0 &&
               text.equals(that.text) &&
               type.equals(that.type);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(text, type, startIndex, endIndex, confidence);
    }
}
