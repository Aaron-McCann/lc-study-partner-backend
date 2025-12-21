package com.studypartner.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_sessions")
@EntityListeners(AuditingEntityListener.class)
public class StudySession {
    
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
    
    private LocalDateTime endTime;
    
    private Integer durationMinutes;
    
    @Enumerated(EnumType.STRING)
    private SessionType type = SessionType.REGULAR;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    // Constructors
    public StudySession() {}
    
    public StudySession(User user, String subject, String topic, LocalDateTime startTime, SessionType type) {
        this.user = user;
        this.subject = subject;
        this.topic = topic;
        this.startTime = startTime;
        this.type = type;
    }
    
    // Helper method to calculate duration when ending session
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
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
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
    
    public SessionType getType() { return type; }
    public void setType(SessionType type) { this.type = type; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public enum SessionType {
        REGULAR, POMODORO, EXAM_PRACTICE
    }
}