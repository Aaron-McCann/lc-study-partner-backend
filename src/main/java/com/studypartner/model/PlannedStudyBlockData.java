package com.studypartner.model;

import java.time.LocalDateTime;

/**
 * POJO for planned study block data (replaces PlannedStudyBlock entity for CSV storage)
 */
public class PlannedStudyBlockData {
    
    private Long id;
    private String subject;
    private String topic;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationHours;
    private String type; // REGULAR, POMODORO, EXAM_PRACTICE
    private String notes;
    private boolean completed = false;
    private LocalDateTime createdAt;
    
    // Constructors
    public PlannedStudyBlockData() {}
    
    public PlannedStudyBlockData(String subject, String topic, LocalDateTime startTime, LocalDateTime endTime) {
        this.subject = subject;
        this.topic = topic;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = calculateDuration();
        this.createdAt = LocalDateTime.now();
    }
    
    // Helper method to calculate duration in hours
    private Integer calculateDuration() {
        if (startTime != null && endTime != null) {
            return (int) java.time.Duration.between(startTime, endTime).toHours();
        }
        return null;
    }
    
    // Helper method to update duration when times change
    public void updateDuration() {
        this.durationHours = calculateDuration();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { 
        this.startTime = startTime; 
        updateDuration();
    }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { 
        this.endTime = endTime; 
        updateDuration();
    }
    
    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}