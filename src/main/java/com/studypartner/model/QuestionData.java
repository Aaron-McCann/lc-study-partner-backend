package com.studypartner.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class QuestionData {
    private Long id;
    private Long paperId;
    private String questionNumber;
    private String content;
    private String questionLink;
    private Integer marks;
    private String topic;
    private String difficulty;
    private String source;
    private String sampleAnswer;
    private LocalDateTime createdAt;
    
    public QuestionData() {
        this.createdAt = LocalDateTime.now();
    }
    
    public QuestionData(Long paperId, String questionNumber, String content, String topic, Integer marks, String difficulty, String source) {
        this();
        this.paperId = paperId;
        this.questionNumber = questionNumber;
        this.content = content;
        this.topic = topic;
        this.marks = marks;
        this.difficulty = difficulty;
        this.source = source;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPaperId() {
        return paperId;
    }
    
    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }
    
    public String getQuestionNumber() {
        return questionNumber;
    }
    
    public void setQuestionNumber(String questionNumber) {
        this.questionNumber = questionNumber;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getQuestionLink() {
        return questionLink;
    }
    
    public void setQuestionLink(String questionLink) {
        this.questionLink = questionLink;
    }
    
    public Integer getMarks() {
        return marks;
    }
    
    public void setMarks(Integer marks) {
        this.marks = marks;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSampleAnswer() {
        return sampleAnswer;
    }
    
    public void setSampleAnswer(String sampleAnswer) {
        this.sampleAnswer = sampleAnswer;
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
        if (!(o instanceof QuestionData)) return false;
        QuestionData that = (QuestionData) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "QuestionData{" +
                "id=" + id +
                ", paperId=" + paperId +
                ", questionNumber='" + questionNumber + '\'' +
                ", topic='" + topic + '\'' +
                ", marks=" + marks +
                ", difficulty='" + difficulty + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}