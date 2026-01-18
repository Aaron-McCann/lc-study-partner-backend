package com.studypartner.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "planned_study_blocks")
@EntityListeners(AuditingEntityListener.class)
public class PlannedStudyBlock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String subject;
    
    private String topic;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    @Column(nullable = false)
    private LocalDateTime endTime;
    
    private Integer durationHours;
    
    @Enumerated(EnumType.STRING)
    private StudySession.SessionType type = StudySession.SessionType.REGULAR;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    private boolean completed = false;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    // Constructors
    public PlannedStudyBlock() {}
    
    public PlannedStudyBlock(User user, String subject, String topic, LocalDateTime startTime, LocalDateTime endTime) {
        this.user = user;
        this.subject = subject;
        this.topic = topic;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = calculateDuration();
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
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
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
    
    public StudySession.SessionType getType() { return type; }
    public void setType(StudySession.SessionType type) { this.type = type; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}