package com.example.numaflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an incoming event from Kafka containing JSON data with title and description fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
    
    @JsonProperty("title")
    @NotBlank(message = "Title is required")
    private String title;
    
    @JsonProperty("description")
    @NotBlank(message = "Description is required")
    private String description;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Additional text fields that might be present
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("summary")
    private String summary;
    
    public Event() {
        this.timestamp = Instant.now();
    }
    
    public Event(String title, String description) {
        this();
        this.title = title;
        this.description = description;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
