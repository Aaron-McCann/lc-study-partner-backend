package com.studypartner.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "achievements")
@EntityListeners(AuditingEntityListener.class)
public class Achievement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String iconName;
    
    @Enumerated(EnumType.STRING)
    private AchievementType type;
    
    private Integer targetValue;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    // Constructors
    public Achievement() {}
    
    public Achievement(String name, String description, String iconName, AchievementType type, Integer targetValue) {
        this.name = name;
        this.description = description;
        this.iconName = iconName;
        this.type = type;
        this.targetValue = targetValue;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    
    public AchievementType getType() { return type; }
    public void setType(AchievementType type) { this.type = type; }
    
    public Integer getTargetValue() { return targetValue; }
    public void setTargetValue(Integer targetValue) { this.targetValue = targetValue; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public enum AchievementType {
        SESSIONS_COMPLETED,      // Number of study sessions
        HOURS_STUDIED,           // Total hours studied
        STREAK_DAYS,             // Consecutive study days
        EARLY_SESSION,           // Session started before certain time
        LONG_SESSION,            // Single session duration
        SUBJECTS_STUDIED,        // Number of different subjects
        WEEKLY_GOAL              // Weekly hour goals met
    }
}