package com.studypartner.repository;

import com.studypartner.entity.PlannedStudyBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlannedStudyBlockRepository extends JpaRepository<PlannedStudyBlock, Long> {
    
    List<PlannedStudyBlock> findByUserIdOrderByStartTimeAsc(Long userId);
    
    @Query("SELECT p FROM PlannedStudyBlock p WHERE p.user.id = :userId " +
           "AND p.startTime >= :startDate AND p.startTime <= :endDate " +
           "ORDER BY p.startTime ASC")
    List<PlannedStudyBlock> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                                    @Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM PlannedStudyBlock p WHERE p.user.id = :userId " +
           "AND DATE(p.startTime) = DATE(:date) " +
           "ORDER BY p.startTime ASC")
    List<PlannedStudyBlock> findByUserIdAndDate(@Param("userId") Long userId, 
                                               @Param("date") LocalDateTime date);
    
    List<PlannedStudyBlock> findByUserIdAndCompletedFalseOrderByStartTimeAsc(Long userId);
}