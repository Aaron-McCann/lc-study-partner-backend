package com.studypartner.service;

import com.studypartner.model.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CSV-based data service that acts as a repository layer.
 * Can be easily replaced with database repositories later.
 */
@Service
public class CsvDataService {
    
    private static final String DATA_DIR = "data/";
    private static final String STUDY_SESSIONS_FILE = DATA_DIR + "study_sessions.csv";
    private static final String PLANNED_BLOCKS_FILE = DATA_DIR + "planned_study_blocks.csv";
    private static final String USER_ACHIEVEMENTS_FILE = DATA_DIR + "user_achievements.csv";
    private static final String ACHIEVEMENT_DEFINITIONS_FILE = DATA_DIR + "achievement_definitions.csv";
    private static final String USER_PROFILE_FILE = DATA_DIR + "user_profile.csv";
    private static final String PAPERS_FILE = DATA_DIR + "papers.csv";
    private static final String QUESTIONS_FILE = DATA_DIR + "questions.csv";
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    // ============= STUDY SESSIONS =============
    
    public List<StudySessionData> getAllStudySessions() {
        return readCsvFile(STUDY_SESSIONS_FILE, this::parseStudySession);
    }
    
    public Optional<StudySessionData> getStudySessionById(Long id) {
        return getAllStudySessions().stream()
                .filter(session -> session.getId().equals(id))
                .findFirst();
    }
    
    public StudySessionData saveStudySession(StudySessionData session) {
        List<StudySessionData> sessions = getAllStudySessions();
        
        if (session.getId() == null) {
            // Generate new ID
            Long maxId = sessions.stream()
                    .mapToLong(StudySessionData::getId)
                    .max().orElse(0L);
            session.setId(maxId + 1);
        } else {
            // Update existing
            sessions.removeIf(s -> s.getId().equals(session.getId()));
        }
        
        sessions.add(session);
        writeCsvFile(STUDY_SESSIONS_FILE, sessions, this::formatStudySession);
        return session;
    }
    
    private StudySessionData parseStudySession(String[] fields) {
        if (fields.length < 9) return null;
        
        StudySessionData session = new StudySessionData();
        session.setId(parseId(fields[0]));
        session.setSubject(fields[1]);
        session.setTopic(fields[2].isEmpty() ? null : fields[2]);
        session.setStartTime(parseDateTime(fields[3]));
        session.setEndTime(parseDateTime(fields[4]));
        session.setDurationMinutes(parseInt(fields[5]));
        session.setType(fields[6]);
        session.setNotes(fields[7].isEmpty() ? null : fields[7]);
        session.setCreatedAt(parseDateTime(fields[8]));
        return session;
    }
    
    private String formatStudySession(StudySessionData session) {
        return String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s",
                session.getId(),
                escapeField(session.getSubject()),
                escapeField(session.getTopic() != null ? session.getTopic() : ""),
                formatDateTime(session.getStartTime()),
                formatDateTime(session.getEndTime()),
                session.getDurationMinutes() != null ? session.getDurationMinutes() : "",
                escapeField(session.getType()),
                escapeField(session.getNotes() != null ? session.getNotes() : ""),
                formatDateTime(session.getCreatedAt())
        );
    }
    
    // ============= PLANNED STUDY BLOCKS =============
    
    public List<PlannedStudyBlockData> getAllPlannedBlocks() {
        return readCsvFile(PLANNED_BLOCKS_FILE, this::parsePlannedBlock);
    }
    
    public Optional<PlannedStudyBlockData> getPlannedBlockById(Long id) {
        return getAllPlannedBlocks().stream()
                .filter(block -> block.getId().equals(id))
                .findFirst();
    }
    
    public PlannedStudyBlockData savePlannedBlock(PlannedStudyBlockData block) {
        List<PlannedStudyBlockData> blocks = getAllPlannedBlocks();
        
        if (block.getId() == null) {
            Long maxId = blocks.stream()
                    .mapToLong(PlannedStudyBlockData::getId)
                    .max().orElse(0L);
            block.setId(maxId + 1);
        } else {
            blocks.removeIf(b -> b.getId().equals(block.getId()));
        }
        
        blocks.add(block);
        writeCsvFile(PLANNED_BLOCKS_FILE, blocks, this::formatPlannedBlock);
        return block;
    }
    
    public void deletePlannedBlock(Long id) {
        List<PlannedStudyBlockData> blocks = getAllPlannedBlocks();
        blocks.removeIf(b -> b.getId().equals(id));
        writeCsvFile(PLANNED_BLOCKS_FILE, blocks, this::formatPlannedBlock);
    }
    
    private PlannedStudyBlockData parsePlannedBlock(String[] fields) {
        if (fields.length < 10) return null;
        
        PlannedStudyBlockData block = new PlannedStudyBlockData();
        block.setId(parseId(fields[0]));
        block.setSubject(fields[1]);
        block.setTopic(fields[2].isEmpty() ? null : fields[2]);
        block.setStartTime(parseDateTime(fields[3]));
        block.setEndTime(parseDateTime(fields[4]));
        block.setDurationHours(parseInt(fields[5]));
        block.setType(fields[6]);
        block.setNotes(fields[7].isEmpty() ? null : fields[7]);
        block.setCompleted(Boolean.parseBoolean(fields[8]));
        block.setCreatedAt(parseDateTime(fields[9]));
        return block;
    }
    
    private String formatPlannedBlock(PlannedStudyBlockData block) {
        return String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                block.getId(),
                escapeField(block.getSubject()),
                escapeField(block.getTopic() != null ? block.getTopic() : ""),
                formatDateTime(block.getStartTime()),
                formatDateTime(block.getEndTime()),
                block.getDurationHours() != null ? block.getDurationHours() : "",
                escapeField(block.getType()),
                escapeField(block.getNotes() != null ? block.getNotes() : ""),
                block.isCompleted(),
                formatDateTime(block.getCreatedAt())
        );
    }
    
    // ============= USER ACHIEVEMENTS =============
    
    public List<UserAchievementData> getAllUserAchievements() {
        return readCsvFile(USER_ACHIEVEMENTS_FILE, this::parseUserAchievement);
    }
    
    public Optional<UserAchievementData> getUserAchievementByAchievementId(Long achievementId) {
        return getAllUserAchievements().stream()
                .filter(ua -> ua.getAchievementId().equals(achievementId))
                .findFirst();
    }
    
    public UserAchievementData saveUserAchievement(UserAchievementData userAchievement) {
        List<UserAchievementData> achievements = getAllUserAchievements();
        achievements.removeIf(ua -> ua.getAchievementId().equals(userAchievement.getAchievementId()));
        achievements.add(userAchievement);
        writeCsvFile(USER_ACHIEVEMENTS_FILE, achievements, this::formatUserAchievement);
        return userAchievement;
    }
    
    private UserAchievementData parseUserAchievement(String[] fields) {
        if (fields.length < 5) return null;
        
        UserAchievementData achievement = new UserAchievementData();
        achievement.setAchievementId(parseId(fields[0]));
        achievement.setAchievementName(fields[1]);
        achievement.setCurrentProgress(parseInt(fields[2]));
        achievement.setUnlocked(Boolean.parseBoolean(fields[3]));
        achievement.setUnlockedAt(parseDateTime(fields[4]));
        return achievement;
    }
    
    private String formatUserAchievement(UserAchievementData achievement) {
        return String.format("%d,%s,%s,%s,%s",
                achievement.getAchievementId(),
                escapeField(achievement.getAchievementName()),
                achievement.getCurrentProgress() != null ? achievement.getCurrentProgress() : "",
                achievement.isUnlocked(),
                formatDateTime(achievement.getUnlockedAt())
        );
    }
    
    // ============= ACHIEVEMENT DEFINITIONS =============
    
    public List<AchievementDefinitionData> getAllAchievementDefinitions() {
        return readCsvFile(ACHIEVEMENT_DEFINITIONS_FILE, this::parseAchievementDefinition);
    }
    
    public Optional<AchievementDefinitionData> getAchievementDefinitionById(Long id) {
        return getAllAchievementDefinitions().stream()
                .filter(def -> def.getId().equals(id))
                .findFirst();
    }
    
    private AchievementDefinitionData parseAchievementDefinition(String[] fields) {
        if (fields.length < 7) return null;
        
        AchievementDefinitionData def = new AchievementDefinitionData();
        def.setId(parseId(fields[0]));
        def.setName(fields[1]);
        def.setDescription(fields[2]);
        def.setIconName(fields[3]);
        def.setType(fields[4]);
        def.setTargetValue(parseInt(fields[5]));
        def.setCreatedAt(parseDateTime(fields[6]));
        return def;
    }
    
    // ============= USER PROFILE =============
    
    public Optional<UserProfileData> getUserProfile() {
        List<UserProfileData> profiles = readCsvFile(USER_PROFILE_FILE, this::parseUserProfile);
        return profiles.isEmpty() ? Optional.empty() : Optional.of(profiles.get(0));
    }
    
    private UserProfileData parseUserProfile(String[] fields) {
        if (fields.length < 8) return null;
        
        UserProfileData profile = new UserProfileData();
        profile.setId(parseId(fields[0]));
        profile.setFirstName(fields[1]);
        profile.setLastName(fields[2]);
        profile.setUsername(fields[3]);
        profile.setEmail(fields[4]);
        profile.setAvatarUrl(fields[5].isEmpty() ? null : fields[5]);
        profile.setRole(fields[6]);
        profile.setCreatedAt(parseDateTime(fields[7]));
        return profile;
    }
    
    // ============= UTILITY METHODS =============
    
    private <T> List<T> readCsvFile(String filename, CsvParser<T> parser) {
        List<T> results = new ArrayList<>();
        File file = new File(filename);
        
        if (!file.exists()) {
            return results;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] fields = parseCsvLine(line);
                T item = parser.parse(fields);
                if (item != null) {
                    results.add(item);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file " + filename + ": " + e.getMessage());
        }
        
        return results;
    }
    
    private <T> void writeCsvFile(String filename, List<T> items, CsvFormatter<T> formatter) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Write header based on file type
            if (filename.contains("study_sessions")) {
                writer.println("id,subject,topic,startTime,endTime,durationMinutes,type,notes,createdAt");
            } else if (filename.contains("planned_study_blocks")) {
                writer.println("id,subject,topic,startTime,endTime,durationHours,type,notes,completed,createdAt");
            } else if (filename.contains("user_achievements")) {
                writer.println("achievementId,achievementName,currentProgress,unlocked,unlockedAt");
            }
            
            for (T item : items) {
                writer.println(formatter.format(item));
            }
        } catch (IOException e) {
            System.err.println("Error writing CSV file " + filename + ": " + e.getMessage());
        }
    }
    
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
    
    private String escapeField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    
    private Long parseId(String value) {
        return value.isEmpty() ? null : Long.parseLong(value);
    }
    
    private Integer parseInt(String value) {
        return value.isEmpty() ? null : Integer.parseInt(value);
    }
    
    private LocalDateTime parseDateTime(String value) {
        return value.isEmpty() ? null : LocalDateTime.parse(value, DATE_FORMAT);
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMAT) : "";
    }
    
    @FunctionalInterface
    private interface CsvParser<T> {
        T parse(String[] fields);
    }
    
    @FunctionalInterface
    private interface CsvFormatter<T> {
        String format(T item);
    }
    
    // ============= PAPERS =============
    
    public List<PaperData> getAllPapers() {
        return readCsvFile(PAPERS_FILE, this::parsePaper);
    }
    
    public Optional<PaperData> getPaperById(Long id) {
        return getAllPapers().stream()
                .filter(paper -> paper.getId().equals(id))
                .findFirst();
    }
    
    public PaperData savePaper(PaperData paper) {
        List<PaperData> papers = getAllPapers();
        
        if (paper.getId() == null) {
            // Generate new ID
            Long maxId = papers.stream()
                    .mapToLong(PaperData::getId)
                    .max().orElse(0L);
            paper.setId(maxId + 1);
        } else {
            // Update existing
            papers.removeIf(p -> p.getId().equals(paper.getId()));
        }
        
        papers.add(paper);
        writeCsvFile(PAPERS_FILE, papers, this::formatPaper);
        return paper;
    }
    
    private PaperData parsePaper(String[] fields) {
        if (fields.length < 6) return null;
        
        PaperData paper = new PaperData();
        paper.setId(parseId(fields[0]));
        paper.setSubject(fields[1]);
        paper.setYear(parseInt(fields[2]));
        paper.setPaper(fields[3]);
        paper.setLevel(fields[4]);
        paper.setDuration(fields[5].isEmpty() ? null : fields[5]);
        paper.setCreatedAt(fields.length > 6 ? parseDateTime(fields[6]) : LocalDateTime.now());
        return paper;
    }
    
    private String formatPaper(PaperData paper) {
        return String.format("%d,%s,%s,%s,%s,%s,%s",
                paper.getId(),
                escapeField(paper.getSubject()),
                paper.getYear() != null ? paper.getYear() : "",
                escapeField(paper.getPaper()),
                escapeField(paper.getLevel()),
                escapeField(paper.getDuration() != null ? paper.getDuration() : ""),
                formatDateTime(paper.getCreatedAt())
        );
    }
    
    // ============= QUESTIONS =============
    
    public List<QuestionData> getAllQuestions() {
        return readCsvFile(QUESTIONS_FILE, this::parseQuestion);
    }
    
    public List<QuestionData> getQuestionsByPaperId(Long paperId) {
        return getAllQuestions().stream()
                .filter(question -> question.getPaperId().equals(paperId))
                .collect(Collectors.toList());
    }
    
    public Optional<QuestionData> getQuestionById(Long id) {
        return getAllQuestions().stream()
                .filter(question -> question.getId().equals(id))
                .findFirst();
    }
    
    public QuestionData saveQuestion(QuestionData question) {
        List<QuestionData> questions = getAllQuestions();
        
        if (question.getId() == null) {
            // Generate new ID
            Long maxId = questions.stream()
                    .mapToLong(QuestionData::getId)
                    .max().orElse(0L);
            question.setId(maxId + 1);
        } else {
            // Update existing
            questions.removeIf(q -> q.getId().equals(question.getId()));
        }
        
        questions.add(question);
        writeCsvFile(QUESTIONS_FILE, questions, this::formatQuestion);
        return question;
    }
    
    private QuestionData parseQuestion(String[] fields) {
        if (fields.length < 10) return null;
        
        QuestionData question = new QuestionData();
        question.setId(parseId(fields[0]));
        question.setPaperId(parseId(fields[1]));
        question.setQuestionNumber(fields[2]);
        question.setContent(fields[3]);
        question.setQuestionLink(fields[4].isEmpty() ? null : fields[4]);
        question.setMarks(parseInt(fields[5]));
        question.setTopic(fields[6]);
        question.setDifficulty(fields[7]);
        question.setSource(fields[8]);
        question.setSampleAnswer(fields[9].isEmpty() ? null : fields[9]);
        question.setCreatedAt(fields.length > 10 ? parseDateTime(fields[10]) : LocalDateTime.now());
        return question;
    }
    
    private String formatQuestion(QuestionData question) {
        return String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                question.getId(),
                question.getPaperId(),
                escapeField(question.getQuestionNumber()),
                escapeField(question.getContent()),
                escapeField(question.getQuestionLink() != null ? question.getQuestionLink() : ""),
                question.getMarks() != null ? question.getMarks() : "",
                escapeField(question.getTopic()),
                escapeField(question.getDifficulty()),
                escapeField(question.getSource()),
                escapeField(question.getSampleAnswer() != null ? question.getSampleAnswer() : ""),
                formatDateTime(question.getCreatedAt())
        );
    }
}