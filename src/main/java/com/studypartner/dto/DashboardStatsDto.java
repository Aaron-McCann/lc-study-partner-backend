package com.studypartner.dto;

public class DashboardStatsDto {
    private Double studyHoursThisWeek;
    private Double studyHoursChange;
    private Integer papersCompleted;
    private Integer totalPapers;
    private Integer papersChange;
    private Integer aiSessions;
    private Integer aiSessionsChange;
    private Integer predictedPoints;
    private Integer predictedPointsChange;
    
    // Constructors
    public DashboardStatsDto() {}
    
    public DashboardStatsDto(Double studyHoursThisWeek, Double studyHoursChange, 
                           Integer papersCompleted, Integer totalPapers, Integer papersChange,
                           Integer aiSessions, Integer aiSessionsChange,
                           Integer predictedPoints, Integer predictedPointsChange) {
        this.studyHoursThisWeek = studyHoursThisWeek;
        this.studyHoursChange = studyHoursChange;
        this.papersCompleted = papersCompleted;
        this.totalPapers = totalPapers;
        this.papersChange = papersChange;
        this.aiSessions = aiSessions;
        this.aiSessionsChange = aiSessionsChange;
        this.predictedPoints = predictedPoints;
        this.predictedPointsChange = predictedPointsChange;
    }
    
    // Getters and Setters
    public Double getStudyHoursThisWeek() { return studyHoursThisWeek; }
    public void setStudyHoursThisWeek(Double studyHoursThisWeek) { this.studyHoursThisWeek = studyHoursThisWeek; }
    
    public Double getStudyHoursChange() { return studyHoursChange; }
    public void setStudyHoursChange(Double studyHoursChange) { this.studyHoursChange = studyHoursChange; }
    
    public Integer getPapersCompleted() { return papersCompleted; }
    public void setPapersCompleted(Integer papersCompleted) { this.papersCompleted = papersCompleted; }
    
    public Integer getTotalPapers() { return totalPapers; }
    public void setTotalPapers(Integer totalPapers) { this.totalPapers = totalPapers; }
    
    public Integer getPapersChange() { return papersChange; }
    public void setPapersChange(Integer papersChange) { this.papersChange = papersChange; }
    
    public Integer getAiSessions() { return aiSessions; }
    public void setAiSessions(Integer aiSessions) { this.aiSessions = aiSessions; }
    
    public Integer getAiSessionsChange() { return aiSessionsChange; }
    public void setAiSessionsChange(Integer aiSessionsChange) { this.aiSessionsChange = aiSessionsChange; }
    
    public Integer getPredictedPoints() { return predictedPoints; }
    public void setPredictedPoints(Integer predictedPoints) { this.predictedPoints = predictedPoints; }
    
    public Integer getPredictedPointsChange() { return predictedPointsChange; }
    public void setPredictedPointsChange(Integer predictedPointsChange) { this.predictedPointsChange = predictedPointsChange; }
}