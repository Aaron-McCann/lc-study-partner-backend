package com.studypartner.service;

import com.studypartner.dto.DashboardStatsDto;
import com.studypartner.repository.StudySessionRepository;
import com.studypartner.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class DashboardService {
    
    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    
    public DashboardService(StudySessionRepository studySessionRepository, 
                          UserRepository userRepository) {
        this.studySessionRepository = studySessionRepository;
        this.userRepository = userRepository;
    }
    
    public DashboardStatsDto getUserStats(Long userId) {
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        LocalDateTime lastWeekStart = LocalDateTime.now().minusDays(14);
        
        // Calculate study hours this week
        Double thisWeekHours = studySessionRepository.getTotalHoursByUserAndTimeRange(userId, weekStart, LocalDateTime.now());
        Double lastWeekHours = studySessionRepository.getTotalHoursByUserAndTimeRange(userId, lastWeekStart, weekStart);
        
        if (thisWeekHours == null) thisWeekHours = 0.0;
        if (lastWeekHours == null) lastWeekHours = 0.0;
        
        Double hoursChange = thisWeekHours - lastWeekHours;
        
        // Mock data for other stats (to be implemented with actual entities)
        return new DashboardStatsDto(
            thisWeekHours,
            hoursChange,
            8, // papersCompleted
            25, // totalPapers
            3, // papersChange
            15, // aiSessions
            5, // aiSessionsChange
            485, // predictedPoints
            12 // predictedPointsChange
        );
    }
    
    public Map<String, Object> getQuickActions(Long userId) {
        Map<String, Object> quickActions = new HashMap<>();
        
        // Mock data for quick actions
        Map<String, Object> resumePaper = new HashMap<>();
        resumePaper.put("id", 1);
        resumePaper.put("name", "Mathematics Paper 2022");
        
        quickActions.put("resumePaper", resumePaper);
        quickActions.put("suggestedTopic", "Quadratic Equations");
        quickActions.put("pendingGoals", 2);
        
        return quickActions;
    }
}