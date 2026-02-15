package com.studypartner.model;

import java.time.LocalDateTime;

public class AchievementData {
    private int id;
    private String name;
    private String description;
    private String iconName;
    private boolean unlocked;
    private int progress;
    private LocalDateTime unlockedAt;
    private String category;
    private int requiredValue;
    
    public AchievementData() {}
    
    public AchievementData(int id, String name, String description, String iconName, 
                          String category, int requiredValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconName = iconName;
        this.category = category;
        this.requiredValue = requiredValue;
        this.unlocked = false;
        this.progress = 0;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public int getRequiredValue() { return requiredValue; }
    public void setRequiredValue(int requiredValue) { this.requiredValue = requiredValue; }
    
    public void updateProgress(int currentValue) {
        this.progress = Math.min(100, (currentValue * 100) / requiredValue);
        if (currentValue >= requiredValue && !unlocked) {
            unlock();
        }
    }
    
    public void unlock() {
        this.unlocked = true;
        this.progress = 100;
        this.unlockedAt = LocalDateTime.now();
    }
}