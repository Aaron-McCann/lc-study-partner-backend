package com.studypartner.model;

import java.time.LocalDateTime;

/**
 * POJO for study session data (replaces StudySession entity for CSV storage)
 * Can be easily converted back to JPA entity when database is implemented
 */
public class StudySessionData {
    
    private Long id;
    private String subject;
    private String topic;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private String type; // REGULAR, POMODORO, EXAM_PRACTICE
    private String notes;
    private LocalDateTime createdAt;
    
    // Constructors
    public StudySessionData() {}
    
    public StudySessionData(String subject, String topic, LocalDateTime startTime, String type) {
        this.subject = subject;
        this.topic = topic;
        this.startTime = startTime;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }
    
    // Helper method to end session and calculate duration
    public void endSession() {
        this.endTime = LocalDateTime.now();
        this.durationMinutes = calculateDuration();
    }
    
    private Integer calculateDuration() {
        if (startTime != null && endTime != null) {
            return (int) java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return null;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}