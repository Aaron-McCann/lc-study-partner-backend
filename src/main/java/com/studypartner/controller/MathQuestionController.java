package com.studypartner.controller;

import com.studypartner.entity.MathQuestion;
import com.studypartner.service.MathQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/questions")
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:8082"})
public class MathQuestionController {
    
    private final MathQuestionService mathQuestionService;
    
    @Autowired
    public MathQuestionController(MathQuestionService mathQuestionService) {
        this.mathQuestionService = mathQuestionService;
    }
    
    @GetMapping
    public ResponseEntity<Page<MathQuestion>> getAllQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<MathQuestion> questions = mathQuestionService.getAllQuestions(pageable);
        return ResponseEntity.ok(questions);
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<MathQuestion>> searchQuestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<MathQuestion> questions = mathQuestionService.searchQuestions(q, pageable);
        return ResponseEntity.ok(questions);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MathQuestion> getQuestionById(@PathVariable Long id) {
        Optional<MathQuestion> question = mathQuestionService.getQuestionById(id);
        return question.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/by-subject/{subject}")
    public ResponseEntity<Page<MathQuestion>> getQuestionsBySubject(
            @PathVariable String subject,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("year").descending().and(Sort.by("questionNumber")));
        Page<MathQuestion> questions = mathQuestionService.getQuestionsBySubject(subject, pageable);
        return ResponseEntity.ok(questions);
    }
    
    @GetMapping("/by-year/{year}")
    public ResponseEntity<Page<MathQuestion>> getQuestionsByYear(
            @PathVariable Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("questionNumber"));
        Page<MathQuestion> questions = mathQuestionService.getQuestionsByYear(year, pageable);
        return ResponseEntity.ok(questions);
    }
    
    @GetMapping("/by-paper-type/{paperType}")
    public ResponseEntity<Page<MathQuestion>> getQuestionsByPaperType(
            @PathVariable String paperType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("year").descending().and(Sort.by("questionNumber")));
        Page<MathQuestion> questions = mathQuestionService.getQuestionsByPaperType(paperType, pageable);
        return ResponseEntity.ok(questions);
    }
    
    @GetMapping("/by-difficulty/{difficulty}")
    public ResponseEntity<Page<MathQuestion>> getQuestionsByDifficulty(
            @PathVariable String difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            MathQuestion.Difficulty diff = MathQuestion.Difficulty.valueOf(difficulty.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by("year").descending().and(Sort.by("questionNumber")));
            Page<MathQuestion> questions = mathQuestionService.getQuestionsByDifficulty(diff, pageable);
            return ResponseEntity.ok(questions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/by-topic/{topic}")
    public ResponseEntity<Page<MathQuestion>> getQuestionsByTopic(
            @PathVariable String topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("year").descending().and(Sort.by("questionNumber")));
        Page<MathQuestion> questions = mathQuestionService.getQuestionsByTopic(topic, pageable);
        return ResponseEntity.ok(questions);
    }
    
    @GetMapping("/paper/{year}/{paperType}")
    public ResponseEntity<List<MathQuestion>> getQuestionsByYearAndPaper(
            @PathVariable Integer year,
            @PathVariable String paperType) {
        
        List<MathQuestion> questions = mathQuestionService.getQuestionsByYearAndPaper(year, paperType);
        return ResponseEntity.ok(questions);
    }
    
    @GetMapping("/subjects")
    public ResponseEntity<List<String>> getAllSubjects() {
        List<String> subjects = mathQuestionService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }
    
    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getAllYears() {
        List<Integer> years = mathQuestionService.getAllYears();
        return ResponseEntity.ok(years);
    }
    
    @GetMapping("/paper-types")
    public ResponseEntity<List<String>> getAllPaperTypes() {
        List<String> paperTypes = mathQuestionService.getAllPaperTypes();
        return ResponseEntity.ok(paperTypes);
    }
    
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getAllTopics() {
        List<String> topics = mathQuestionService.getAllTopics();
        return ResponseEntity.ok(topics);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuestionStatistics() {
        Map<String, Object> stats = mathQuestionService.getQuestionStatistics();
        return ResponseEntity.ok(stats);
    }
}