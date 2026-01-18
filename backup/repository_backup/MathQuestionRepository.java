package com.studypartner.repository;

import com.studypartner.entity.MathQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MathQuestionRepository extends JpaRepository<MathQuestion, Long> {
    
    Page<MathQuestion> findBySubjectIgnoreCase(String subject, Pageable pageable);
    
    Page<MathQuestion> findByYear(Integer year, Pageable pageable);
    
    Page<MathQuestion> findByPaperTypeIgnoreCase(String paperType, Pageable pageable);
    
    Page<MathQuestion> findByDifficulty(MathQuestion.Difficulty difficulty, Pageable pageable);
    
    Page<MathQuestion> findByPrimaryTopicContainingIgnoreCase(String topic, Pageable pageable);
    
    @Query("SELECT q FROM MathQuestion q WHERE " +
           "LOWER(q.questionText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(q.primaryTopic) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(q.allTopics) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<MathQuestion> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT q FROM MathQuestion q WHERE q.year = :year AND q.paperType = :paperType ORDER BY q.questionNumber, q.subPart")
    List<MathQuestion> findByYearAndPaperTypeOrderByQuestionNumber(@Param("year") Integer year, @Param("paperType") String paperType);
    
    @Query("SELECT DISTINCT q.subject FROM MathQuestion q ORDER BY q.subject")
    List<String> findAllSubjects();
    
    @Query("SELECT DISTINCT q.year FROM MathQuestion q ORDER BY q.year DESC")
    List<Integer> findAllYears();
    
    @Query("SELECT DISTINCT q.paperType FROM MathQuestion q ORDER BY q.paperType")
    List<String> findAllPaperTypes();
    
    @Query("SELECT DISTINCT q.primaryTopic FROM MathQuestion q WHERE q.primaryTopic IS NOT NULL ORDER BY q.primaryTopic")
    List<String> findAllPrimaryTopics();
    
    long countBySubjectIgnoreCase(String subject);
    
    long countByYear(Integer year);
    
    long countByDifficulty(MathQuestion.Difficulty difficulty);
    
    @Query("SELECT q.difficulty, COUNT(q) FROM MathQuestion q GROUP BY q.difficulty")
    List<Object[]> countByDifficultyGrouped();
    
    @Query("SELECT q.year, COUNT(q) FROM MathQuestion q GROUP BY q.year ORDER BY q.year")
    List<Object[]> countByYearGrouped();
}