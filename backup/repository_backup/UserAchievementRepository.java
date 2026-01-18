package com.studypartner.repository;

import com.studypartner.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    
    List<UserAchievement> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<UserAchievement> findByUserIdAndUnlockedTrueOrderByUnlockedAtDesc(Long userId);
    
    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);
    
    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.unlocked = true")
    Long countUnlockedByUserId(@Param("userId") Long userId);
}