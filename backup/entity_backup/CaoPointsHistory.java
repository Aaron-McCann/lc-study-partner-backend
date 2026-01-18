package com.studypartner.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "cao_points_history")
@EntityListeners(AuditingEntityListener.class)
public class CaoPointsHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cao_code", nullable = false)
    private CaoCourse caoCourse;
    
    @Column(name = "year_value", nullable = false)
    private Integer year;
    
    @Column(name = "round_1_points")
    private Integer round1Points;
    
    @Column(name = "round_2_points")
    private Integer round2Points;
    
    @Column(name = "round_3_points")
    private Integer round3Points;
    
    @Column(name = "average_points")
    private Double averagePoints;
    
    @Column(name = "minimum_points")
    private Integer minimumPoints;
    
    @Column(name = "maximum_points")
    private Integer maximumPoints;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public CaoPointsHistory() {}
    
    public CaoPointsHistory(CaoCourse caoCourse, Integer year) {
        this.caoCourse = caoCourse;
        this.year = year;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public CaoCourse getCaoCourse() { return caoCourse; }
    public void setCaoCourse(CaoCourse caoCourse) { this.caoCourse = caoCourse; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public Integer getRound1Points() { return round1Points; }
    public void setRound1Points(Integer round1Points) { this.round1Points = round1Points; }
    
    public Integer getRound2Points() { return round2Points; }
    public void setRound2Points(Integer round2Points) { this.round2Points = round2Points; }
    
    public Integer getRound3Points() { return round3Points; }
    public void setRound3Points(Integer round3Points) { this.round3Points = round3Points; }
    
    public Double getAveragePoints() { return averagePoints; }
    public void setAveragePoints(Double averagePoints) { this.averagePoints = averagePoints; }
    
    public Integer getMinimumPoints() { return minimumPoints; }
    public void setMinimumPoints(Integer minimumPoints) { this.minimumPoints = minimumPoints; }
    
    public Integer getMaximumPoints() { return maximumPoints; }
    public void setMaximumPoints(Integer maximumPoints) { this.maximumPoints = maximumPoints; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}