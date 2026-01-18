package com.studypartner.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cao_courses")
@EntityListeners(AuditingEntityListener.class)
public class CaoCourse {
    
    @Id
    @Column(name = "cao_code", length = 10)
    private String caoCode;
    
    @NotBlank
    @Size(max = 200)
    @Column(name = "course_name", nullable = false)
    private String courseName;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "institution", nullable = false)
    private String institution;
    
    @Size(max = 100)
    @Column(name = "location")
    private String location;
    
    @Column(name = "nfq_level")
    private Integer nfqLevel;
    
    @Size(max = 50)
    @Column(name = "duration")
    private String duration;
    
    @Size(max = 500)
    @Column(name = "categories")
    private String categories;
    
    @Size(max = 500)
    @Column(name = "course_url")
    private String courseUrl;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @OneToMany(mappedBy = "caoCourse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaoPointsHistory> pointsHistory = new ArrayList<>();
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public CaoCourse() {}
    
    public CaoCourse(String caoCode, String courseName, String institution) {
        this.caoCode = caoCode;
        this.courseName = courseName;
        this.institution = institution;
    }
    
    // Getters and Setters
    public String getCaoCode() { return caoCode; }
    public void setCaoCode(String caoCode) { this.caoCode = caoCode; }
    
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public Integer getNfqLevel() { return nfqLevel; }
    public void setNfqLevel(Integer nfqLevel) { this.nfqLevel = nfqLevel; }
    
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    
    public String getCourseUrl() { return courseUrl; }
    public void setCourseUrl(String courseUrl) { this.courseUrl = courseUrl; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<CaoPointsHistory> getPointsHistory() { return pointsHistory; }
    public void setPointsHistory(List<CaoPointsHistory> pointsHistory) { this.pointsHistory = pointsHistory; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}