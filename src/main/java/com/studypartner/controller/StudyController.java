package com.studypartner.controller;

import com.studypartner.entity.StudySession;
import com.studypartner.entity.User;
import com.studypartner.entity.Achievement;
import com.studypartner.entity.UserAchievement;
import com.studypartner.entity.PlannedStudyBlock;
import com.studypartner.repository.StudySessionRepository;
import com.studypartner.repository.UserRepository;
import com.studypartner.repository.AchievementRepository;
import com.studypartner.repository.UserAchievementRepository;
import com.studypartner.repository.PlannedStudyBlockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/study")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://localhost:8082"})
public class StudyController {
    
    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PlannedStudyBlockRepository plannedStudyBlockRepository;
    
    // For now, use a hardcoded test user ID
    private static final Long TEST_USER_ID = 1L;
    
    public StudyController(StudySessionRepository studySessionRepository, 
                          UserRepository userRepository,
                          AchievementRepository achievementRepository,
                          UserAchievementRepository userAchievementRepository,
                          PlannedStudyBlockRepository plannedStudyBlockRepository) {
        this.studySessionRepository = studySessionRepository;
        this.userRepository = userRepository;
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.plannedStudyBlockRepository = plannedStudyBlockRepository;
    }
    
    /**
     * Start a new study session
     * POST /api/study/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<?> startSession(@RequestBody StartSessionRequest request) {
        try {
            // Get test user
            Optional<User> userOpt = userRepository.findById(TEST_USER_ID);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Test user not found");
            }
            
            User user = userOpt.get();
            
            // Convert string type to enum
            StudySession.SessionType sessionType;
            try {
                sessionType = StudySession.SessionType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                sessionType = StudySession.SessionType.REGULAR;
            }
            
            // Create new session
            StudySession session = new StudySession();
            session.setUser(user);
            session.setSubject(request.getSubject());
            session.setTopic(request.getTopic());
            session.setStartTime(LocalDateTime.now());
            session.setType(sessionType);
            
            StudySession savedSession = studySessionRepository.save(session);
            
            return ResponseEntity.ok(convertToDto(savedSession));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error starting session: " + e.getMessage());
        }
    }
    
    /**
     * End an active study session
     * PUT /api/study/sessions/{id}/end
     */
    @PutMapping("/sessions/{id}/end")
    public ResponseEntity<?> endSession(@PathVariable Long id, @RequestBody EndSessionRequest request) {
        try {
            Optional<StudySession> sessionOpt = studySessionRepository.findById(id);
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            StudySession session = sessionOpt.get();
            
            // Check if session belongs to test user
            if (!session.getUser().getId().equals(TEST_USER_ID)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Session does not belong to current user");
            }
            
            // End the session
            session.endSession();
            if (request.getNotes() != null) {
                session.setNotes(request.getNotes());
            }
            
            StudySession savedSession = studySessionRepository.save(session);
            
            // Update achievement progress after ending session
            updateAchievementProgress();
            
            return ResponseEntity.ok(convertToDto(savedSession));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error ending session: " + e.getMessage());
        }
    }
    
    /**
     * Get today's study sessions
     * GET /api/study/sessions/today
     */
    @GetMapping("/sessions/today")
    public ResponseEntity<List<StudySessionDto>> getTodaySessions() {
        try {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            
            List<StudySession> sessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(TEST_USER_ID)
                .stream()
                .filter(session -> 
                    session.getStartTime().isAfter(startOfDay) && 
                    session.getStartTime().isBefore(endOfDay)
                )
                .toList();
            
            List<StudySessionDto> sessionDtos = sessions.stream()
                .map(this::convertToDto)
                .toList();
            
            return ResponseEntity.ok(sessionDtos);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get study sessions for a date range
     * GET /api/study/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<StudySessionDto>> getSessions(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            
            List<StudySession> sessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(TEST_USER_ID)
                .stream()
                .filter(session -> 
                    session.getStartTime().isAfter(start) && 
                    session.getStartTime().isBefore(end)
                )
                .toList();
            
            List<StudySessionDto> sessionDtos = sessions.stream()
                .map(this::convertToDto)
                .toList();
            
            return ResponseEntity.ok(sessionDtos);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get study statistics
     * GET /api/study/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<StudyStatsDto> getStats() {
        try {
            LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
            
            // Get total hours this week
            Double totalHours = studySessionRepository.getTotalHoursByUserAndTimeRange(
                TEST_USER_ID, weekStart, LocalDateTime.now());
            
            // Get total sessions count
            Long totalSessions = studySessionRepository.countSessionsByUserSince(TEST_USER_ID, 
                LocalDateTime.of(1970, 1, 1, 0, 0));
            
            // Get all sessions for calculations
            List<StudySession> allSessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(TEST_USER_ID);
            
            // Calculate average session length
            double averageLength = allSessions.stream()
                .filter(session -> session.getDurationMinutes() != null)
                .mapToInt(StudySession::getDurationMinutes)
                .average()
                .orElse(0.0);
            
            // Calculate subject breakdown
            Map<String, Double> subjectHours = new HashMap<>();
            double totalMinutes = 0.0;
            
            for (StudySession session : allSessions) {
                if (session.getDurationMinutes() != null) {
                    double hours = session.getDurationMinutes() / 60.0;
                    subjectHours.merge(session.getSubject(), hours, Double::sum);
                    totalMinutes += session.getDurationMinutes();
                }
            }
            
            // Find top subject
            String topSubject = subjectHours.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
            
            // Calculate streak
            StudyStreakDto streak = calculateStreak(allSessions);
            
            StudyStatsDto stats = new StudyStatsDto();
            stats.setTotalHoursThisWeek(totalHours != null ? totalHours : 0.0);
            stats.setTotalSessions(totalSessions != null ? totalSessions.intValue() : 0);
            stats.setAverageSessionLength(averageLength);
            stats.setTopSubject(topSubject);
            stats.setStreak(streak);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get subject breakdown statistics (alias for frontend compatibility)
     * GET /api/study/stats/subjects
     */
    @GetMapping("/stats/subjects")
    public ResponseEntity<List<SubjectBreakdownDto>> getStatsSubjects() {
        return getSubjectBreakdown();
    }
    
    /**
     * Get subject breakdown statistics
     * GET /api/study/subject-breakdown
     */
    @GetMapping("/subject-breakdown")
    public ResponseEntity<List<SubjectBreakdownDto>> getSubjectBreakdown() {
        try {
            List<StudySession> allSessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(TEST_USER_ID);
            
            // Calculate subject hours
            Map<String, Double> subjectHours = new HashMap<>();
            double totalMinutes = 0.0;
            
            for (StudySession session : allSessions) {
                if (session.getDurationMinutes() != null) {
                    double hours = session.getDurationMinutes() / 60.0;
                    subjectHours.merge(session.getSubject(), hours, Double::sum);
                    totalMinutes += session.getDurationMinutes();
                }
            }
            
            double totalHours = totalMinutes / 60.0;
            
            // Convert to DTOs with percentages
            List<SubjectBreakdownDto> breakdown = subjectHours.entrySet().stream()
                .map(entry -> {
                    SubjectBreakdownDto dto = new SubjectBreakdownDto();
                    dto.setSubject(entry.getKey());
                    dto.setHours(entry.getValue());
                    dto.setPercentage(totalHours > 0 ? (int) Math.round((entry.getValue() / totalHours) * 100) : 0);
                    return dto;
                })
                .sorted((a, b) -> Double.compare(b.getHours(), a.getHours()))
                .toList();
            
            return ResponseEntity.ok(breakdown);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get current study streak
     * GET /api/study/streak
     */
    @GetMapping("/streak")
    public ResponseEntity<StudyStreakDto> getStreak() {
        try {
            List<StudySession> allSessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(TEST_USER_ID);
            StudyStreakDto streak = calculateStreak(allSessions);
            return ResponseEntity.ok(streak);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper method to calculate study streak
    private StudyStreakDto calculateStreak(List<StudySession> sessions) {
        if (sessions.isEmpty()) {
            StudyStreakDto streak = new StudyStreakDto();
            streak.setCurrentStreak(0);
            streak.setLongestStreak(0);
            streak.setLastStudyDate(LocalDateTime.now().toString());
            streak.setWeeklyProgress(new boolean[]{false, false, false, false, false, false, false});
            return streak;
        }
        
        // Get unique study dates (ignoring time)
        Set<LocalDate> studyDates = sessions.stream()
            .map(session -> session.getStartTime().toLocalDate())
            .collect(Collectors.toSet());
        
        List<LocalDate> sortedDates = studyDates.stream()
            .sorted()
            .toList();
        
        // Calculate current streak
        int currentStreak = 0;
        LocalDate today = LocalDate.now();
        LocalDate checkDate = today;
        
        // Check if studied today or yesterday (to account for late night sessions)
        if (!studyDates.contains(today) && !studyDates.contains(today.minusDays(1))) {
            currentStreak = 0;
        } else {
            // Start from today or yesterday if studied
            if (studyDates.contains(today)) {
                checkDate = today;
            } else {
                checkDate = today.minusDays(1);
            }
            
            // Count consecutive days backwards
            while (studyDates.contains(checkDate)) {
                currentStreak++;
                checkDate = checkDate.minusDays(1);
            }
        }
        
        // Calculate longest streak
        int longestStreak = 0;
        int tempStreak = 1;
        
        if (!sortedDates.isEmpty()) {
            for (int i = 1; i < sortedDates.size(); i++) {
                LocalDate prev = sortedDates.get(i - 1);
                LocalDate current = sortedDates.get(i);
                
                if (prev.plusDays(1).equals(current)) {
                    tempStreak++;
                } else {
                    longestStreak = Math.max(longestStreak, tempStreak);
                    tempStreak = 1;
                }
            }
            longestStreak = Math.max(longestStreak, tempStreak);
        }
        
        // Calculate weekly progress (last 7 days)
        boolean[] weeklyProgress = new boolean[7];
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i); // Start from 6 days ago
            weeklyProgress[i] = studyDates.contains(day);
        }
        
        // Get last study date
        String lastStudyDate = sortedDates.isEmpty() ? 
            LocalDateTime.now().toString() : 
            sortedDates.get(sortedDates.size() - 1).atStartOfDay().toString();
        
        StudyStreakDto streak = new StudyStreakDto();
        streak.setCurrentStreak(currentStreak);
        streak.setLongestStreak(longestStreak);
        streak.setLastStudyDate(lastStudyDate);
        streak.setWeeklyProgress(weeklyProgress);
        
        return streak;
    }
    
    // Helper method to calculate achievement progress percentage
    private Integer calculateProgress(Achievement achievement, Integer currentValue) {
        if (achievement.getTargetValue() == null || achievement.getTargetValue() == 0) {
            return currentValue != null && currentValue > 0 ? 100 : 0;
        }
        
        if (currentValue == null) {
            currentValue = 0;
        }
        
        return Math.min(100, (int) Math.round((currentValue.doubleValue() / achievement.getTargetValue()) * 100));
    }
    
    // Helper method to update achievement progress (call after study actions)
    private void updateAchievementProgress() {
        try {
            Optional<User> userOpt = userRepository.findById(TEST_USER_ID);
            if (userOpt.isEmpty()) {
                return;
            }
            
            User user = userOpt.get();
            List<StudySession> allSessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(TEST_USER_ID);
            
            // Calculate current user stats
            int totalSessions = allSessions.size();
            double totalHours = allSessions.stream()
                .filter(session -> session.getDurationMinutes() != null)
                .mapToDouble(session -> session.getDurationMinutes() / 60.0)
                .sum();
            
            StudyStreakDto streak = calculateStreak(allSessions);
            
            // Update achievements based on current stats
            List<Achievement> achievements = achievementRepository.findAllByOrderByCreatedAtAsc();
            
            for (Achievement achievement : achievements) {
                UserAchievement userAchievement = userAchievementRepository
                    .findByUserIdAndAchievementId(TEST_USER_ID, achievement.getId())
                    .orElse(new UserAchievement(user, achievement));
                
                if (!userAchievement.isUnlocked()) {
                    Integer currentProgress = calculateCurrentProgress(achievement, totalSessions, totalHours, streak);
                    userAchievement.setCurrentProgress(currentProgress);
                    
                    // Check if achievement should be unlocked
                    if (achievement.getTargetValue() != null && currentProgress >= achievement.getTargetValue()) {
                        userAchievement.unlock();
                    } else if (achievement.getTargetValue() == null && currentProgress > 0) {
                        userAchievement.unlock();
                    }
                    
                    userAchievementRepository.save(userAchievement);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Error updating achievements: " + e.getMessage());
        }
    }
    
    // Helper method to calculate current progress for an achievement
    private Integer calculateCurrentProgress(Achievement achievement, int totalSessions, double totalHours, StudyStreakDto streak) {
        return switch (achievement.getType()) {
            case SESSIONS_COMPLETED -> totalSessions;
            case HOURS_STUDIED -> (int) Math.round(totalHours);
            case STREAK_DAYS -> streak.getCurrentStreak();
            case EARLY_SESSION -> hasEarlySession() ? 1 : 0;
            default -> 0;
        };
    }
    
    // Helper method to check if user has any early morning sessions
    private boolean hasEarlySession() {
        List<StudySession> allSessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(TEST_USER_ID);
        return allSessions.stream()
            .anyMatch(session -> session.getStartTime().getHour() < 7);
    }
    
    /**
     * Get all achievements with user progress
     * GET /api/study/achievements
     */
    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementDto>> getAchievements() {
        try {
            List<Achievement> allAchievements = achievementRepository.findAllByOrderByCreatedAtAsc();
            List<UserAchievement> userAchievements = userAchievementRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
            
            // Create a map for quick lookup of user achievements
            Map<Long, UserAchievement> userAchievementMap = userAchievements.stream()
                .collect(Collectors.toMap(
                    ua -> ua.getAchievement().getId(),
                    ua -> ua
                ));
            
            // Convert to DTOs with user progress
            List<AchievementDto> achievementDtos = allAchievements.stream()
                .map(achievement -> {
                    UserAchievement userAchievement = userAchievementMap.get(achievement.getId());
                    
                    AchievementDto dto = new AchievementDto();
                    dto.setId(achievement.getId());
                    dto.setName(achievement.getName());
                    dto.setDescription(achievement.getDescription());
                    dto.setIconName(achievement.getIconName());
                    
                    if (userAchievement != null) {
                        dto.setUnlocked(userAchievement.isUnlocked());
                        dto.setProgress(calculateProgress(achievement, userAchievement.getCurrentProgress()));
                        dto.setUnlockedAt(userAchievement.getUnlockedAt() != null ? 
                            userAchievement.getUnlockedAt().toString() : null);
                    } else {
                        dto.setUnlocked(false);
                        dto.setProgress(calculateProgress(achievement, 0));
                        dto.setUnlockedAt(null);
                    }
                    
                    return dto;
                })
                .toList();
            
            return ResponseEntity.ok(achievementDtos);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get weekly progress (placeholder implementation)
     * GET /api/study/weekly-progress
     */
    @GetMapping("/weekly-progress")
    public ResponseEntity<boolean[]> getWeeklyProgress() {
        boolean[] progress = {false, false, false, false, false, false, false};
        return ResponseEntity.ok(progress);
    }
    
    /**
     * Log a completed pomodoro session (placeholder implementation)
     * POST /api/study/pomodoro
     */
    @PostMapping("/pomodoro")
    public ResponseEntity<ApiResponseDto> logPomodoro(@RequestBody PomodoroRequest request) {
        // For now, just return success
        ApiResponseDto response = new ApiResponseDto();
        response.setSuccess(true);
        response.setMessage("Pomodoro session logged successfully");
        
        return ResponseEntity.ok(response);
    }
    
    // ==========================================
    // Planned Study Block Endpoints
    // ==========================================
    
    /**
     * Get planned study blocks for a date range
     * GET /api/study/planned-blocks
     */
    @GetMapping("/planned-blocks")
    public ResponseEntity<List<PlannedStudyBlockDto>> getPlannedBlocks(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            
            List<PlannedStudyBlock> blocks = plannedStudyBlockRepository.findByUserIdAndDateRange(
                TEST_USER_ID, start, end);
            
            List<PlannedStudyBlockDto> blockDtos = blocks.stream()
                .map(this::convertToPlannedBlockDto)
                .toList();
            
            return ResponseEntity.ok(blockDtos);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create a new planned study block
     * POST /api/study/planned-blocks
     */
    @PostMapping("/planned-blocks")
    public ResponseEntity<?> createPlannedBlock(@RequestBody CreatePlannedBlockRequest request) {
        try {
            Optional<User> userOpt = userRepository.findById(TEST_USER_ID);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Test user not found");
            }
            
            User user = userOpt.get();
            
            PlannedStudyBlock block = new PlannedStudyBlock();
            block.setUser(user);
            block.setSubject(request.getSubject());
            block.setTopic(request.getTopic());
            block.setStartTime(LocalDateTime.parse(request.getStartTime()));
            block.setEndTime(LocalDateTime.parse(request.getEndTime()));
            block.setNotes(request.getNotes());
            
            if (request.getType() != null) {
                try {
                    block.setType(StudySession.SessionType.valueOf(request.getType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    block.setType(StudySession.SessionType.REGULAR);
                }
            }
            
            PlannedStudyBlock savedBlock = plannedStudyBlockRepository.save(block);
            
            return ResponseEntity.ok(convertToPlannedBlockDto(savedBlock));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating planned block: " + e.getMessage());
        }
    }
    
    /**
     * Update a planned study block
     * PUT /api/study/planned-blocks/{id}
     */
    @PutMapping("/planned-blocks/{id}")
    public ResponseEntity<?> updatePlannedBlock(@PathVariable Long id, @RequestBody CreatePlannedBlockRequest request) {
        try {
            Optional<PlannedStudyBlock> blockOpt = plannedStudyBlockRepository.findById(id);
            if (blockOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PlannedStudyBlock block = blockOpt.get();
            
            // Check if block belongs to test user
            if (!block.getUser().getId().equals(TEST_USER_ID)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Block does not belong to current user");
            }
            
            // Update fields
            if (request.getSubject() != null) {
                block.setSubject(request.getSubject());
            }
            if (request.getTopic() != null) {
                block.setTopic(request.getTopic());
            }
            if (request.getStartTime() != null) {
                block.setStartTime(LocalDateTime.parse(request.getStartTime()));
            }
            if (request.getEndTime() != null) {
                block.setEndTime(LocalDateTime.parse(request.getEndTime()));
            }
            if (request.getNotes() != null) {
                block.setNotes(request.getNotes());
            }
            if (request.getType() != null) {
                try {
                    block.setType(StudySession.SessionType.valueOf(request.getType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Keep existing type if invalid
                }
            }
            
            PlannedStudyBlock savedBlock = plannedStudyBlockRepository.save(block);
            
            return ResponseEntity.ok(convertToPlannedBlockDto(savedBlock));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating planned block: " + e.getMessage());
        }
    }
    
    /**
     * Delete a planned study block
     * DELETE /api/study/planned-blocks/{id}
     */
    @DeleteMapping("/planned-blocks/{id}")
    public ResponseEntity<?> deletePlannedBlock(@PathVariable Long id) {
        try {
            Optional<PlannedStudyBlock> blockOpt = plannedStudyBlockRepository.findById(id);
            if (blockOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PlannedStudyBlock block = blockOpt.get();
            
            // Check if block belongs to test user
            if (!block.getUser().getId().equals(TEST_USER_ID)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Block does not belong to current user");
            }
            
            plannedStudyBlockRepository.delete(block);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting planned block: " + e.getMessage());
        }
    }
    
    /**
     * Mark a planned study block as completed
     * PUT /api/study/planned-blocks/{id}/complete
     */
    @PutMapping("/planned-blocks/{id}/complete")
    public ResponseEntity<?> completePlannedBlock(@PathVariable Long id) {
        try {
            Optional<PlannedStudyBlock> blockOpt = plannedStudyBlockRepository.findById(id);
            if (blockOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PlannedStudyBlock block = blockOpt.get();
            
            // Check if block belongs to test user
            if (!block.getUser().getId().equals(TEST_USER_ID)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Block does not belong to current user");
            }
            
            block.setCompleted(true);
            PlannedStudyBlock savedBlock = plannedStudyBlockRepository.save(block);
            
            return ResponseEntity.ok(convertToPlannedBlockDto(savedBlock));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error completing planned block: " + e.getMessage());
        }
    }
    
    // Helper method to convert entity to DTO
    private StudySessionDto convertToDto(StudySession session) {
        StudySessionDto dto = new StudySessionDto();
        dto.setId(session.getId());
        dto.setUserId(session.getUser().getId());
        dto.setSubject(session.getSubject());
        dto.setTopic(session.getTopic());
        dto.setStartTime(session.getStartTime().toString());
        dto.setEndTime(session.getEndTime() != null ? session.getEndTime().toString() : null);
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setType(session.getType().name().toLowerCase());
        dto.setNotes(session.getNotes());
        dto.setCreatedAt(session.getCreatedAt().toString());
        
        return dto;
    }
    
    // Helper method to convert PlannedStudyBlock to DTO
    private PlannedStudyBlockDto convertToPlannedBlockDto(PlannedStudyBlock block) {
        PlannedStudyBlockDto dto = new PlannedStudyBlockDto();
        dto.setId(block.getId().toString());
        dto.setSubject(block.getSubject());
        dto.setTopic(block.getTopic());
        dto.setStartTime(block.getStartTime().toString());
        dto.setEndTime(block.getEndTime().toString());
        
        // Calculate day of week (0-6, Monday=0)
        int dayOfWeek = block.getStartTime().getDayOfWeek().getValue() - 1;
        dto.setDay(dayOfWeek);
        
        // Calculate start hour and duration
        dto.setStartHour(block.getStartTime().getHour());
        dto.setDuration(block.getDurationHours() != null ? block.getDurationHours() : 1);
        
        return dto;
    }
    
    /**
     * Get current study streak (separate endpoint for frontend compatibility)
     * GET /api/study/streak
     */
    @GetMapping("/streak")
    public ResponseEntity<StudyStreakDto> getStreak() {
        try {
            Map<String, Object> streakData = calculateStreak();
            
            StudyStreakDto streakDto = new StudyStreakDto();
            streakDto.setCurrentStreak((Integer) streakData.get("currentStreak"));
            streakDto.setLongestStreak((Integer) streakData.get("longestStreak"));
            streakDto.setLastStudyDate((String) streakData.get("lastStudyDate"));
            streakDto.setWeeklyProgress((boolean[]) streakData.get("weeklyProgress"));
            
            return ResponseEntity.ok(streakDto);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get weekly progress (separate endpoint for frontend compatibility)
     * GET /api/study/weekly-progress
     */
    @GetMapping("/weekly-progress")
    public ResponseEntity<boolean[]> getWeeklyProgress() {
        try {
            Map<String, Object> streakData = calculateStreak();
            boolean[] weeklyProgress = (boolean[]) streakData.get("weeklyProgress");
            
            return ResponseEntity.ok(weeklyProgress);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request/Response DTOs
    public static class StartSessionRequest {
        private String subject;
        private String topic;
        private String type;
        
        // Getters and setters
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
    
    public static class EndSessionRequest {
        private String notes;
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    public static class PomodoroRequest {
        private String subject;
        private int durationMinutes;
        private boolean isBreak;
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
        public boolean isBreak() { return isBreak; }
        public void setBreak(boolean isBreak) { this.isBreak = isBreak; }
    }
    
    public static class StudySessionDto {
        private Long id;
        private Long userId;
        private String subject;
        private String topic;
        private String startTime;
        private String endTime;
        private Integer durationMinutes;
        private String type;
        private String notes;
        private String createdAt;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    public static class StudyStatsDto {
        private double totalHoursThisWeek;
        private int totalSessions;
        private double averageSessionLength;
        private String topSubject;
        private StudyStreakDto streak;
        
        public double getTotalHoursThisWeek() { return totalHoursThisWeek; }
        public void setTotalHoursThisWeek(double totalHoursThisWeek) { this.totalHoursThisWeek = totalHoursThisWeek; }
        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
        public double getAverageSessionLength() { return averageSessionLength; }
        public void setAverageSessionLength(double averageSessionLength) { this.averageSessionLength = averageSessionLength; }
        public String getTopSubject() { return topSubject; }
        public void setTopSubject(String topSubject) { this.topSubject = topSubject; }
        public StudyStreakDto getStreak() { return streak; }
        public void setStreak(StudyStreakDto streak) { this.streak = streak; }
    }
    
    public static class StudyStreakDto {
        private int currentStreak;
        private int longestStreak;
        private String lastStudyDate;
        private boolean[] weeklyProgress;
        
        public int getCurrentStreak() { return currentStreak; }
        public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
        public int getLongestStreak() { return longestStreak; }
        public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
        public String getLastStudyDate() { return lastStudyDate; }
        public void setLastStudyDate(String lastStudyDate) { this.lastStudyDate = lastStudyDate; }
        public boolean[] getWeeklyProgress() { return weeklyProgress; }
        public void setWeeklyProgress(boolean[] weeklyProgress) { this.weeklyProgress = weeklyProgress; }
    }
    
    public static class AchievementDto {
        private Long id;
        private String name;
        private String description;
        private String iconName;
        private boolean unlocked;
        private Integer progress;
        private String unlockedAt;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIconName() { return iconName; }
        public void setIconName(String iconName) { this.iconName = iconName; }
        public boolean isUnlocked() { return unlocked; }
        public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }
        public String getUnlockedAt() { return unlockedAt; }
        public void setUnlockedAt(String unlockedAt) { this.unlockedAt = unlockedAt; }
    }
    
    public static class ApiResponseDto {
        private boolean success;
        private String message;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class SubjectBreakdownDto {
        private String subject;
        private double hours;
        private int percentage;
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public double getHours() { return hours; }
        public void setHours(double hours) { this.hours = hours; }
        public int getPercentage() { return percentage; }
        public void setPercentage(int percentage) { this.percentage = percentage; }
    }
    
    public static class PlannedStudyBlockDto {
        private String id;
        private String subject;
        private String topic;
        private String startTime;
        private String endTime;
        private int startHour;
        private int duration;
        private int day;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public int getStartHour() { return startHour; }
        public void setStartHour(int startHour) { this.startHour = startHour; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
    }
    
    public static class CreatePlannedBlockRequest {
        private String subject;
        private String topic;
        private String startTime;
        private String endTime;
        private String type;
        private String notes;
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    public static class StudyStreakDto {
        private int currentStreak;
        private int longestStreak;
        private String lastStudyDate;
        private boolean[] weeklyProgress;
        
        public int getCurrentStreak() { return currentStreak; }
        public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
        public int getLongestStreak() { return longestStreak; }
        public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
        public String getLastStudyDate() { return lastStudyDate; }
        public void setLastStudyDate(String lastStudyDate) { this.lastStudyDate = lastStudyDate; }
        public boolean[] getWeeklyProgress() { return weeklyProgress; }
        public void setWeeklyProgress(boolean[] weeklyProgress) { this.weeklyProgress = weeklyProgress; }
    }
}