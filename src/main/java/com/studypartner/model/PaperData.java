package com.studypartner.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class PaperData {
    private Long id;
    private String subject;
    private Integer year;
    private String paper;
    private String level;
    private String duration;
    private LocalDateTime createdAt;
    
    public PaperData() {
        this.createdAt = LocalDateTime.now();
    }
    
    public PaperData(String subject, Integer year, String paper, String level) {
        this();
        this.subject = subject;
        this.year = year;
        this.paper = paper;
        this.level = level;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public String getPaper() {
        return paper;
    }
    
    public void setPaper(String paper) {
        this.paper = paper;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperData)) return false;
        PaperData paperData = (PaperData) o;
        return Objects.equals(id, paperData.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "PaperData{" +
                "id=" + id +
                ", subject='" + subject + '\'' +
                ", year=" + year +
                ", paper='" + paper + '\'' +
                ", level='" + level + '\'' +
                '}';
    }
}