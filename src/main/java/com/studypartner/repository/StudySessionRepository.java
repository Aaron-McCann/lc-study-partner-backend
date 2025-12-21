package com.studypartner.repository;

import com.studypartner.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    
    List<StudySession> findByUserIdOrderByStartTimeDesc(Long userId);
    
    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) / 60.0 FROM StudySession s " +
           "WHERE s.user.id = :userId AND s.startTime >= :startDate AND s.startTime <= :endDate")
    Double getTotalHoursByUserAndTimeRange(@Param("userId") Long userId, 
                                         @Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(s) FROM StudySession s WHERE s.user.id = :userId AND s.startTime >= :startDate")
    Long countSessionsByUserSince(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
}