package com.studypartner.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "math_questions")
@EntityListeners(AuditingEntityListener.class)
public class MathQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "subject", nullable = false)
    private String subject;
    
    @NotNull
    @Column(name = "year_value", nullable = false)
    private Integer year;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "paper_type", nullable = false)
    private String paperType;
    
    @NotNull
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;
    
    @Size(max = 10)
    @Column(name = "sub_part")
    private String subPart;
    
    @NotBlank
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(name = "full_question_text", columnDefinition = "TEXT")
    private String fullQuestionText;
    
    @Column(name = "images", columnDefinition = "TEXT")
    private String images;
    
    @Column(name = "marks")
    private Integer marks;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private Difficulty difficulty;
    
    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;
    
    @Column(name = "page_number")
    private Integer pageNumber;
    
    @Column(name = "position_in_paper")
    private Integer positionInPaper;
    
    @Size(max = 100)
    @Column(name = "text_hash")
    private String textHash;
    
    @Size(max = 100)
    @Column(name = "primary_topic")
    private String primaryTopic;
    
    @Column(name = "all_topics", columnDefinition = "TEXT")
    private String allTopics;
    
    @Column(name = "topic_count")
    private Integer topicCount;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public MathQuestion() {}
    
    public MathQuestion(String subject, Integer year, String paperType, Integer questionNumber, String questionText) {
        this.subject = subject;
        this.year = year;
        this.paperType = paperType;
        this.questionNumber = questionNumber;
        this.questionText = questionText;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public String getPaperType() { return paperType; }
    public void setPaperType(String paperType) { this.paperType = paperType; }
    
    public Integer getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(Integer questionNumber) { this.questionNumber = questionNumber; }
    
    public String getSubPart() { return subPart; }
    public void setSubPart(String subPart) { this.subPart = subPart; }
    
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    
    public String getFullQuestionText() { return fullQuestionText; }
    public void setFullQuestionText(String fullQuestionText) { this.fullQuestionText = fullQuestionText; }
    
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    
    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }
    
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    
    public Integer getPositionInPaper() { return positionInPaper; }
    public void setPositionInPaper(Integer positionInPaper) { this.positionInPaper = positionInPaper; }
    
    public String getTextHash() { return textHash; }
    public void setTextHash(String textHash) { this.textHash = textHash; }
    
    public String getPrimaryTopic() { return primaryTopic; }
    public void setPrimaryTopic(String primaryTopic) { this.primaryTopic = primaryTopic; }
    
    public String getAllTopics() { return allTopics; }
    public void setAllTopics(String allTopics) { this.allTopics = allTopics; }
    
    public Integer getTopicCount() { return topicCount; }
    public void setTopicCount(Integer topicCount) { this.topicCount = topicCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
}