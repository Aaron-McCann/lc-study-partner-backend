package com.studypartner.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "papers")
@EntityListeners(AuditingEntityListener.class)
public class Paper {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(name = "year_value", nullable = false)
    private Integer year;
    
    @Column(nullable = false)
    private String paper; // e.g., "Paper 1", "Paper 2"
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;
    
    @ElementCollection
    @CollectionTable(name = "paper_topics", joinColumns = @JoinColumn(name = "paper_id"))
    @Column(name = "topic")
    private List<String> topics = new ArrayList<>();
    
    @Column(nullable = false)
    private String duration; // e.g., "3 hours"
    
    private String downloadUrl;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    // Constructors
    public Paper() {}
    
    public Paper(String subject, Integer year, String paper, Level level, String duration) {
        this.subject = subject;
        this.year = year;
        this.paper = paper;
        this.level = level;
        this.duration = duration;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public String getPaper() { return paper; }
    public void setPaper(String paper) { this.paper = paper; }
    
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
    
    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }
    
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public enum Level {
        HIGHER, ORDINARY
    }
}