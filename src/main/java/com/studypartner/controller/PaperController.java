package com.studypartner.controller;

import com.studypartner.model.*;
import com.studypartner.service.CsvDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/papers")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://localhost:8082"}, 
            allowedHeaders = "*", 
            methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class PaperController {
    
    private final CsvDataService csvDataService;
    
    public PaperController(CsvDataService csvDataService) {
        this.csvDataService = csvDataService;
    }
    
    /**
     * Get all papers with optional filters
     * GET /api/papers
     */
    @GetMapping
    public ResponseEntity<List<PaperDto>> getPapers(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Boolean completed) {
        try {
            List<PaperData> papers = csvDataService.getAllPapers();
            
            // Apply filters
            if (subject != null) {
                papers = papers.stream()
                    .filter(p -> p.getSubject().equalsIgnoreCase(subject))
                    .collect(Collectors.toList());
            }
            if (year != null) {
                papers = papers.stream()
                    .filter(p -> Objects.equals(p.getYear(), year))
                    .collect(Collectors.toList());
            }
            if (level != null) {
                papers = papers.stream()
                    .filter(p -> p.getLevel().equalsIgnoreCase(level))
                    .collect(Collectors.toList());
            }
            
            List<PaperDto> paperDtos = papers.stream()
                .map(this::convertToPaperDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(paperDtos);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get a specific paper by ID
     * GET /api/papers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaperDto> getPaperById(@PathVariable Long id) {
        try {
            Optional<PaperData> paperOpt = csvDataService.getPaperById(id);
            if (paperOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PaperDto paperDto = convertToPaperDto(paperOpt.get());
            return ResponseEntity.ok(paperDto);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get questions for a specific paper
     * GET /api/papers/{id}/questions
     */
    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuestionDto>> getPaperQuestions(@PathVariable Long id) {
        try {
            List<QuestionData> questions = csvDataService.getQuestionsByPaperId(id);
            
            List<QuestionDto> questionDtos = questions.stream()
                .map(this::convertToQuestionDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(questionDtos);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get available subjects
     * GET /api/papers/subjects
     */
    @GetMapping("/subjects")
    public ResponseEntity<List<String>> getSubjects() {
        try {
            List<PaperData> papers = csvDataService.getAllPapers();
            List<String> subjects = papers.stream()
                .map(PaperData::getSubject)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(subjects);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get available years
     * GET /api/papers/years
     */
    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getYears() {
        try {
            List<PaperData> papers = csvDataService.getAllPapers();
            List<Integer> years = papers.stream()
                .map(PaperData::getYear)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(years);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Mark a paper as completed
     * POST /api/papers/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponseDto> markPaperCompleted(@PathVariable Long id) {
        try {
            // For now, just return success (completion tracking can be added later)
            ApiResponseDto response = new ApiResponseDto();
            response.setSuccess(true);
            response.setMessage("Paper marked as completed");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get paper completion stats
     * GET /api/papers/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<PaperStatsDto> getPaperStats() {
        try {
            List<PaperData> papers = csvDataService.getAllPapers();
            
            PaperStatsDto stats = new PaperStatsDto();
            stats.setTotal(papers.size());
            stats.setCompleted(0); // Placeholder - completion tracking to be added
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper method to convert PaperData to DTO
    private PaperDto convertToPaperDto(PaperData paper) {
        PaperDto dto = new PaperDto();
        dto.setId(paper.getId());
        dto.setSubject(paper.getSubject());
        dto.setYear(paper.getYear());
        dto.setPaper(paper.getPaper());
        dto.setLevel(paper.getLevel());
        dto.setDuration(paper.getDuration());
        dto.setCompleted(false); // Placeholder - completion tracking to be added
        
        // Get topics for this paper
        List<QuestionData> questions = csvDataService.getQuestionsByPaperId(paper.getId());
        List<String> topics = questions.stream()
            .map(QuestionData::getTopic)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        dto.setTopics(topics);
        
        return dto;
    }
    
    // Helper method to convert QuestionData to DTO
    private QuestionDto convertToQuestionDto(QuestionData question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setPaperId(question.getPaperId());
        dto.setQuestionNumber(question.getQuestionNumber());
        dto.setContent(question.getContent());
        dto.setQuestionLink(question.getQuestionLink());
        dto.setMarks(question.getMarks());
        dto.setTopic(question.getTopic());
        dto.setDifficulty(question.getDifficulty());
        dto.setSampleAnswer(question.getSampleAnswer());
        
        return dto;
    }
    
    // DTOs for API responses
    public static class PaperDto {
        private Long id;
        private String subject;
        private Integer year;
        private String paper;
        private String level;
        private List<String> topics;
        private boolean completed;
        private String duration;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public String getPaper() { return paper; }
        public void setPaper(String paper) { this.paper = paper; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public List<String> getTopics() { return topics; }
        public void setTopics(List<String> topics) { this.topics = topics; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
    }
    
    public static class QuestionDto {
        private Long id;
        private Long paperId;
        private String questionNumber;
        private String content;
        private String questionLink;
        private Integer marks;
        private String topic;
        private String difficulty;
        private String sampleAnswer;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPaperId() { return paperId; }
        public void setPaperId(Long paperId) { this.paperId = paperId; }
        public String getQuestionNumber() { return questionNumber; }
        public void setQuestionNumber(String questionNumber) { this.questionNumber = questionNumber; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getQuestionLink() { return questionLink; }
        public void setQuestionLink(String questionLink) { this.questionLink = questionLink; }
        public Integer getMarks() { return marks; }
        public void setMarks(Integer marks) { this.marks = marks; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public String getSampleAnswer() { return sampleAnswer; }
        public void setSampleAnswer(String sampleAnswer) { this.sampleAnswer = sampleAnswer; }
    }
    
    public static class PaperStatsDto {
        private int total;
        private int completed;
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getCompleted() { return completed; }
        public void setCompleted(int completed) { this.completed = completed; }
    }
    
    public static class ApiResponseDto {
        private boolean success;
        private String message;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}