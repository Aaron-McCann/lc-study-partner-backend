package com.studypartner.model;

import java.time.LocalDateTime;

/**
 * POJO for achievement definition data (replaces Achievement entity for CSV storage)
 */
public class AchievementDefinitionData {
    
    private Long id;
    private String name;
    private String description;
    private String iconName;
    private String type; // SESSIONS_COMPLETED, HOURS_STUDIED, STREAK_DAYS, etc.
    private Integer targetValue;
    private LocalDateTime createdAt;
    
    // Constructors
    public AchievementDefinitionData() {}
    
    public AchievementDefinitionData(String name, String description, String iconName, String type, Integer targetValue) {
        this.name = name;
        this.description = description;
        this.iconName = iconName;
        this.type = type;
        this.targetValue = targetValue;
        this.createdAt = LocalDateTime.now();
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
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Integer getTargetValue() { return targetValue; }
    public void setTargetValue(Integer targetValue) { this.targetValue = targetValue; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}