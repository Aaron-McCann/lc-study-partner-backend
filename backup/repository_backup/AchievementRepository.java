package com.studypartner.repository;

import com.studypartner.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    
    List<Achievement> findAllByOrderByCreatedAtAsc();
    
    List<Achievement> findByType(Achievement.AchievementType type);
}