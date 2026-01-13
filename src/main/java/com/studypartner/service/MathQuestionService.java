package com.studypartner.service;

import com.studypartner.entity.MathQuestion;
import com.studypartner.repository.MathQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MathQuestionService {
    
    private final MathQuestionRepository mathQuestionRepository;
    
    @Autowired
    public MathQuestionService(MathQuestionRepository mathQuestionRepository) {
        this.mathQuestionRepository = mathQuestionRepository;
    }
    
    public Page<MathQuestion> getAllQuestions(Pageable pageable) {
        return mathQuestionRepository.findAll(pageable);
    }
    
    public Optional<MathQuestion> getQuestionById(Long id) {
        return mathQuestionRepository.findById(id);
    }
    
    public Page<MathQuestion> searchQuestions(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return mathQuestionRepository.findAll(pageable);
        }
        return mathQuestionRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }
    
    public Page<MathQuestion> getQuestionsBySubject(String subject, Pageable pageable) {
        return mathQuestionRepository.findBySubjectIgnoreCase(subject, pageable);
    }
    
    public Page<MathQuestion> getQuestionsByYear(Integer year, Pageable pageable) {
        return mathQuestionRepository.findByYear(year, pageable);
    }
    
    public Page<MathQuestion> getQuestionsByPaperType(String paperType, Pageable pageable) {
        return mathQuestionRepository.findByPaperTypeIgnoreCase(paperType, pageable);
    }
    
    public Page<MathQuestion> getQuestionsByDifficulty(MathQuestion.Difficulty difficulty, Pageable pageable) {
        return mathQuestionRepository.findByDifficulty(difficulty, pageable);
    }
    
    public Page<MathQuestion> getQuestionsByTopic(String topic, Pageable pageable) {
        return mathQuestionRepository.findByPrimaryTopicContainingIgnoreCase(topic, pageable);
    }
    
    public List<MathQuestion> getQuestionsByYearAndPaper(Integer year, String paperType) {
        return mathQuestionRepository.findByYearAndPaperTypeOrderByQuestionNumber(year, paperType);
    }
    
    public List<String> getAllSubjects() {
        return mathQuestionRepository.findAllSubjects();
    }
    
    public List<Integer> getAllYears() {
        return mathQuestionRepository.findAllYears();
    }
    
    public List<String> getAllPaperTypes() {
        return mathQuestionRepository.findAllPaperTypes();
    }
    
    public List<String> getAllTopics() {
        return mathQuestionRepository.findAllPrimaryTopics();
    }
    
    public Map<String, Object> getQuestionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalQuestions", mathQuestionRepository.count());
        
        // Difficulty distribution
        List<Object[]> difficultyStats = mathQuestionRepository.countByDifficultyGrouped();
        Map<String, Long> difficultyMap = new HashMap<>();
        for (Object[] row : difficultyStats) {
            difficultyMap.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("difficultyDistribution", difficultyMap);
        
        // Year distribution
        List<Object[]> yearStats = mathQuestionRepository.countByYearGrouped();
        Map<Integer, Long> yearMap = new HashMap<>();
        for (Object[] row : yearStats) {
            yearMap.put((Integer) row[0], (Long) row[1]);
        }
        stats.put("yearDistribution", yearMap);
        
        return stats;
    }
    
    public MathQuestion saveQuestion(MathQuestion question) {
        return mathQuestionRepository.save(question);
    }
    
    public void deleteQuestion(Long id) {
        mathQuestionRepository.deleteById(id);
    }
    
    public long getTotalQuestions() {
        return mathQuestionRepository.count();
    }
    
    public long getQuestionCountBySubject(String subject) {
        return mathQuestionRepository.countBySubjectIgnoreCase(subject);
    }
    
    public long getQuestionCountByYear(Integer year) {
        return mathQuestionRepository.countByYear(year);
    }
    
    public long getQuestionCountByDifficulty(MathQuestion.Difficulty difficulty) {
        return mathQuestionRepository.countByDifficulty(difficulty);
    }
}