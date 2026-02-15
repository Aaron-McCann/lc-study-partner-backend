package com.studypartner.controller;

import com.studypartner.model.*;
import com.studypartner.service.CsvDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/study")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://localhost:8082"})
public class StudyController {
    
    private final CsvDataService csvDataService;
    
    public StudyController(CsvDataService csvDataService) {
        this.csvDataService = csvDataService;
    }
    
    /**
     * Start a new study session
     * POST /api/study/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<?> startSession(@RequestBody StartSessionRequest request) {
        try {
            StudySessionData session = new StudySessionData(
                request.getSubject(),
                request.getTopic(),
                LocalDateTime.now(),
                request.getType() != null ? request.getType().toUpperCase() : "REGULAR"
            );
            
            StudySessionData savedSession = csvDataService.saveStudySession(session);
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
            Optional<StudySessionData> sessionOpt = csvDataService.getStudySessionById(id);
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            StudySessionData session = sessionOpt.get();
            session.endSession();
            if (request.getNotes() != null) {
                session.setNotes(request.getNotes());
            }
            
            StudySessionData savedSession = csvDataService.saveStudySession(session);
            
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
            
            List<StudySessionData> sessions = csvDataService.getAllStudySessions()
                .stream()
                .filter(session -> 
                    session.getStartTime().isAfter(startOfDay) && 
                    session.getStartTime().isBefore(endOfDay)
                )
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
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
            
            List<StudySessionData> sessions = csvDataService.getAllStudySessions()
                .stream()
                .filter(session -> 
                    session.getStartTime().isAfter(start) && 
                    session.getStartTime().isBefore(end)
                )
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
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
            List<StudySessionData> allSessions = csvDataService.getAllStudySessions();
            
            // Get total hours this week
            double totalHours = allSessions.stream()
                .filter(session -> session.getStartTime().isAfter(weekStart))
                .filter(session -> session.getDurationMinutes() != null)
                .mapToDouble(session -> session.getDurationMinutes() / 60.0)
                .sum();
            
            // Get total sessions count
            int totalSessions = allSessions.size();
            
            // Calculate average session length
            double averageLength = allSessions.stream()
                .filter(session -> session.getDurationMinutes() != null)
                .mapToInt(StudySessionData::getDurationMinutes)
                .average()
                .orElse(0.0);
            
            // Calculate subject breakdown
            Map<String, Double> subjectHours = new HashMap<>();
            for (StudySessionData session : allSessions) {
                if (session.getDurationMinutes() != null) {
                    double hours = session.getDurationMinutes() / 60.0;
                    subjectHours.merge(session.getSubject(), hours, Double::sum);
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
            stats.setTotalHoursThisWeek(totalHours);
            stats.setTotalSessions(totalSessions);
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
            List<StudySessionData> allSessions = csvDataService.getAllStudySessions();
            
            // Calculate subject hours
            Map<String, Double> subjectHours = new HashMap<>();
            double totalMinutes = 0.0;
            
            for (StudySessionData session : allSessions) {
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
            List<StudySessionData> allSessions = csvDataService.getAllStudySessions();
            StudyStreakDto streak = calculateStreak(allSessions);
            return ResponseEntity.ok(streak);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all achievements with user progress
     * GET /api/study/achievements
     */
    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementDto>> getAchievements() {
        try {
            List<AchievementDefinitionData> allAchievements = csvDataService.getAllAchievementDefinitions();
            List<UserAchievementData> userAchievements = csvDataService.getAllUserAchievements();
            
            // Create a map for quick lookup of user achievements
            Map<Long, UserAchievementData> userAchievementMap = userAchievements.stream()
                .collect(Collectors.toMap(
                    UserAchievementData::getAchievementId,
                    ua -> ua
                ));
            
            // Convert to DTOs with user progress
            List<AchievementDto> achievementDtos = allAchievements.stream()
                .map(achievement -> {
                    UserAchievementData userAchievement = userAchievementMap.get(achievement.getId());
                    
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
            
            List<PlannedStudyBlockData> blocks = csvDataService.getAllPlannedBlocks()
                .stream()
                .filter(block -> 
                    block.getStartTime().isAfter(start.minusSeconds(1)) && 
                    block.getStartTime().isBefore(end.plusSeconds(1))
                )
                .toList();
            
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
            PlannedStudyBlockData block = new PlannedStudyBlockData();
            block.setSubject(request.getSubject());
            block.setTopic(request.getTopic());
            block.setStartTime(LocalDateTime.parse(request.getStartTime()));
            block.setEndTime(LocalDateTime.parse(request.getEndTime()));
            block.setNotes(request.getNotes());
            block.setType(request.getType() != null ? request.getType().toUpperCase() : "REGULAR");
            block.setCreatedAt(LocalDateTime.now());
            
            PlannedStudyBlockData savedBlock = csvDataService.savePlannedBlock(block);
            
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
            Optional<PlannedStudyBlockData> blockOpt = csvDataService.getPlannedBlockById(id);
            if (blockOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PlannedStudyBlockData block = blockOpt.get();
            
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
                block.setType(request.getType().toUpperCase());
            }
            
            PlannedStudyBlockData savedBlock = csvDataService.savePlannedBlock(block);
            
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
            Optional<PlannedStudyBlockData> blockOpt = csvDataService.getPlannedBlockById(id);
            if (blockOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            csvDataService.deletePlannedBlock(id);
            
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
            Optional<PlannedStudyBlockData> blockOpt = csvDataService.getPlannedBlockById(id);
            if (blockOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PlannedStudyBlockData block = blockOpt.get();
            block.setCompleted(true);
            PlannedStudyBlockData savedBlock = csvDataService.savePlannedBlock(block);
            
            return ResponseEntity.ok(convertToPlannedBlockDto(savedBlock));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error completing planned block: " + e.getMessage());
        }
    }
    
    // ============= HELPER METHODS =============
    
    // Helper method to calculate study streak
    private StudyStreakDto calculateStreak(List<StudySessionData> sessions) {
        // Include question completion dates in streak calculation
        List<QuestionCompletionData> completions = csvDataService.getAllQuestionCompletions();
        
        // Get unique study dates from both sessions and question completions
        Set<LocalDate> studyDates = new HashSet<>();
        
        // Add session dates
        studyDates.addAll(sessions.stream()
            .map(session -> session.getStartTime().toLocalDate())
            .collect(Collectors.toSet()));
        
        // Add question completion dates
        studyDates.addAll(completions.stream()
            .map(completion -> completion.getCompletedAt().toLocalDate())
            .collect(Collectors.toSet()));
        
        if (studyDates.isEmpty()) {
            StudyStreakDto streak = new StudyStreakDto();
            streak.setCurrentStreak(0);
            streak.setLongestStreak(0);
            streak.setLastStudyDate(LocalDateTime.now().toString());
            streak.setWeeklyProgress(new boolean[]{false, false, false, false, false, false, false});
            return streak;
        }
        
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
    private Integer calculateProgress(AchievementDefinitionData achievement, Integer currentValue) {
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
            List<StudySessionData> allSessions = csvDataService.getAllStudySessions();
            List<QuestionCompletionData> allQuestions = csvDataService.getAllQuestionCompletions();
            
            // Calculate current user stats
            int totalSessions = allSessions.size();
            double totalHours = allSessions.stream()
                .filter(session -> session.getDurationMinutes() != null)
                .mapToDouble(session -> session.getDurationMinutes() / 60.0)
                .sum();
            
            StudyStreakDto streak = calculateStreak(allSessions);
            int mathQuestions = (int) allQuestions.stream()
                .filter(q -> "Mathematics".equalsIgnoreCase(q.getSubject()))
                .count();
            int totalQuestions = allQuestions.size();
            
            // Update specific achievements
            csvDataService.updateAchievementProgress(1, totalSessions); // First Steps
            csvDataService.updateAchievementProgress(2, totalSessions); // Getting Started  
            csvDataService.updateAchievementProgress(3, streak.getCurrentStreak()); // Study Streak
            csvDataService.updateAchievementProgress(4, (int) totalHours); // Dedicated Learner
            csvDataService.updateAchievementProgress(5, mathQuestions); // Math Master
            csvDataService.updateAchievementProgress(6, totalQuestions); // Question Solver
            csvDataService.updateAchievementProgress(8, streak.getCurrentStreak()); // Consistent Student
            
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Error updating achievements: " + e.getMessage());
        }
    }
    
    @GetMapping("/achievements")
    public ResponseEntity<?> getAchievements() {
        try {
            List<AchievementData> achievements = csvDataService.getAllAchievements();
            
            // Convert to DTOs
            List<Map<String, Object>> achievementDtos = achievements.stream()
                .map(achievement -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", achievement.getId());
                    dto.put("name", achievement.getName());
                    dto.put("description", achievement.getDescription());
                    dto.put("iconName", achievement.getIconName());
                    dto.put("unlocked", achievement.isUnlocked());
                    dto.put("progress", achievement.getProgress());
                    dto.put("unlockedAt", achievement.getUnlockedAt() != null ? achievement.getUnlockedAt().toString() : null);
                    return dto;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(achievementDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving achievements: " + e.getMessage());
        }
    }
    
    @GetMapping("/user-goals")
    public ResponseEntity<?> getUserGoals() {
        try {
            UserProfileData userProfile = csvDataService.getUserProfile();
            
            Map<String, Object> goals = new HashMap<>();
            goals.put("dailyGoalQuestions", userProfile.getDailyGoalQuestions());
            goals.put("dailyGoalMinutes", userProfile.getDailyGoalMinutes());
            
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving user goals: " + e.getMessage());
        }
    }
    
    @PostMapping("/user-goals")
    public ResponseEntity<?> updateUserGoals(@RequestBody Map<String, Integer> goalRequest) {
        try {
            UserProfileData userProfile = csvDataService.getUserProfile();
            
            if (goalRequest.containsKey("dailyGoalQuestions")) {
                int questions = goalRequest.get("dailyGoalQuestions");
                if (questions > 0 && questions <= 50) { // Reasonable limits
                    userProfile.setDailyGoalQuestions(questions);
                }
            }
            
            if (goalRequest.containsKey("dailyGoalMinutes")) {
                int minutes = goalRequest.get("dailyGoalMinutes");
                if (minutes > 0 && minutes <= 480) { // Max 8 hours per day
                    userProfile.setDailyGoalMinutes(minutes);
                }
            }
            
            csvDataService.saveUserProfile(userProfile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("dailyGoalQuestions", userProfile.getDailyGoalQuestions());
            response.put("dailyGoalMinutes", userProfile.getDailyGoalMinutes());
            response.put("message", "Goals updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating user goals: " + e.getMessage());
        }
    }
    
    // Helper method to convert entity to DTO
    private StudySessionDto convertToDto(StudySessionData session) {
        StudySessionDto dto = new StudySessionDto();
        dto.setId(session.getId());
        dto.setUserId(1L); // Single user ID
        dto.setSubject(session.getSubject());
        dto.setTopic(session.getTopic());
        dto.setStartTime(session.getStartTime().toString());
        dto.setEndTime(session.getEndTime() != null ? session.getEndTime().toString() : null);
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setType(session.getType().toLowerCase());
        dto.setNotes(session.getNotes());
        dto.setCreatedAt(session.getCreatedAt().toString());
        
        return dto;
    }
    
    // Helper method to convert PlannedStudyBlock to DTO
    private PlannedStudyBlockDto convertToPlannedBlockDto(PlannedStudyBlockData block) {
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

    /**
     * Get today's study progress for streak banner
     * GET /api/study/today-progress
     */
    @GetMapping("/today-progress")
    public ResponseEntity<TodayProgressDto> getTodayProgress() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
            
            // Count today's question completions
            List<QuestionCompletionData> todayCompletions = csvDataService.getAllQuestionCompletions()
                .stream()
                .filter(completion -> 
                    completion.getCompletedAt().isAfter(startOfDay) && 
                    completion.getCompletedAt().isBefore(endOfDay)
                )
                .toList();
            
            // Calculate streak
            List<StudySessionData> allSessions = csvDataService.getAllStudySessions();
            StudyStreakDto streak = calculateStreak(allSessions);
            
            // Get user's daily goal from profile
            UserProfileData userProfile = csvDataService.getUserProfile();
            
            TodayProgressDto progress = new TodayProgressDto();
            progress.setCurrentStreak(streak.getCurrentStreak());
            progress.setQuestionsToday(todayCompletions.size());
            progress.setDailyGoal(userProfile.getDailyGoalQuestions()); // Dynamic daily goal
            
            return ResponseEntity.ok(progress);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get weekly progress array for streak display
     * GET /api/study/weekly-progress
     */
    @GetMapping("/weekly-progress")
    public ResponseEntity<boolean[]> getWeeklyProgress() {
        try {
            List<StudySessionData> allSessions = csvDataService.getAllStudySessions();
            StudyStreakDto streak = calculateStreak(allSessions);
            
            return ResponseEntity.ok(streak.getWeeklyProgress());
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request/Response DTOs (reusing from original controller)
    public static class StartSessionRequest {
        private String subject;
        private String topic;
        private String type;
        
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

    public static class TodayProgressDto {
        private int currentStreak;
        private int questionsToday;
        private int dailyGoal;
        
        public int getCurrentStreak() { return currentStreak; }
        public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
        public int getQuestionsToday() { return questionsToday; }
        public void setQuestionsToday(int questionsToday) { this.questionsToday = questionsToday; }
        public int getDailyGoal() { return dailyGoal; }
        public void setDailyGoal(int dailyGoal) { this.dailyGoal = dailyGoal; }
    }
}