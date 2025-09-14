package com.example.numaflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a text segment (sentence) with associated named entities.
 */
public class TextSegment {
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("startIndex")
    private int startIndex;
    
    @JsonProperty("endIndex")
    private int endIndex;
    
    @JsonProperty("namedEntities")
    private List<NamedEntity> namedEntities;
    
    @JsonProperty("segmentNumber")
    private int segmentNumber;
    
    public TextSegment() {
        this.namedEntities = new ArrayList<>();
    }
    
    public TextSegment(String text, int startIndex, int endIndex, int segmentNumber) {
        this();
        this.text = text;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.segmentNumber = segmentNumber;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
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
    
    public List<NamedEntity> getNamedEntities() {
        return namedEntities;
    }
    
    public void setNamedEntities(List<NamedEntity> namedEntities) {
        this.namedEntities = namedEntities != null ? namedEntities : new ArrayList<>();
    }
    
    public void addNamedEntity(NamedEntity entity) {
        if (this.namedEntities == null) {
            this.namedEntities = new ArrayList<>();
        }
        this.namedEntities.add(entity);
    }
    
    public int getSegmentNumber() {
        return segmentNumber;
    }
    
    public void setSegmentNumber(int segmentNumber) {
        this.segmentNumber = segmentNumber;
    }
    
    @Override
    public String toString() {
        return "TextSegment{" +
                "segmentNumber=" + segmentNumber +
                ", text='" + text + '\'' +
                ", startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                ", namedEntities=" + namedEntities +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TextSegment that = (TextSegment) obj;
        return startIndex == that.startIndex &&
               endIndex == that.endIndex &&
               segmentNumber == that.segmentNumber &&
               text.equals(that.text);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(text, startIndex, endIndex, segmentNumber);
    }
}
