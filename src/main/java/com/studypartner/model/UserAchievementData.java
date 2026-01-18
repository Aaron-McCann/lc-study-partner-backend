package com.studypartner.model;

import java.time.LocalDateTime;

/**
 * POJO for user achievement data (replaces UserAchievement entity for CSV storage)
 */
public class UserAchievementData {
    
    private Long achievementId;
    private String achievementName;
    private Integer currentProgress;
    private boolean unlocked = false;
    private LocalDateTime unlockedAt;
    
    // Constructors
    public UserAchievementData() {}
    
    public UserAchievementData(Long achievementId, String achievementName) {
        this.achievementId = achievementId;
        this.achievementName = achievementName;
        this.currentProgress = 0;
    }
    
    // Helper method to unlock achievement
    public void unlock() {
        this.unlocked = true;
        this.unlockedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }
    
    public String getAchievementName() { return achievementName; }
    public void setAchievementName(String achievementName) { this.achievementName = achievementName; }
    
    public Integer getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(Integer currentProgress) { this.currentProgress = currentProgress; }
    
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}