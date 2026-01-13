package com.studypartner.config;

import com.studypartner.entity.CaoCourse;
import com.studypartner.entity.CaoPointsHistory;
import com.studypartner.entity.MathQuestion;
import com.studypartner.entity.User;
import com.studypartner.entity.Achievement;
import com.studypartner.repository.CaoCourseRepository;
import com.studypartner.repository.CaoPointsHistoryRepository;
import com.studypartner.repository.MathQuestionRepository;
import com.studypartner.repository.UserRepository;
import com.studypartner.repository.AchievementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DataLoader implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    
    private final CaoCourseRepository caoCourseRepository;
    private final CaoPointsHistoryRepository pointsHistoryRepository;
    private final MathQuestionRepository mathQuestionRepository;
    private final UserRepository userRepository;
    private final AchievementRepository achievementRepository;
    
    @Autowired
    public DataLoader(CaoCourseRepository caoCourseRepository,
                     CaoPointsHistoryRepository pointsHistoryRepository,
                     MathQuestionRepository mathQuestionRepository,
                     UserRepository userRepository,
                     AchievementRepository achievementRepository) {
        this.caoCourseRepository = caoCourseRepository;
        this.pointsHistoryRepository = pointsHistoryRepository;
        this.mathQuestionRepository = mathQuestionRepository;
        this.userRepository = userRepository;
        this.achievementRepository = achievementRepository;
    }
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Create test user first
        if (userRepository.count() == 0) {
            logger.info("Creating test user...");
            createTestUser();
            logger.info("Test user created successfully!");
        }
        
        if (caoCourseRepository.count() == 0) {
            logger.info("Loading CAO courses data...");
            loadCaoCoursesData();
            logger.info("CAO courses data loaded successfully!");
        }
        
        if (mathQuestionRepository.count() == 0) {
            logger.info("Loading math questions data...");
            loadMathQuestionsData();
            logger.info("Math questions data loaded successfully!");
        }
        
        if (achievementRepository.count() == 0) {
            logger.info("Loading sample achievements...");
            createSampleAchievements();
            logger.info("Sample achievements loaded successfully!");
        }
    }
    
    private void loadCaoCoursesData() {
        try {
            ClassPathResource resource = new ClassPathResource("data/cao_courses_fixed.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                if (fields.length >= 10) {
                    CaoCourse course = new CaoCourse();
                    course.setCaoCode(fields[0]);
                    course.setCourseName(fields[1]);
                    course.setInstitution(fields[2]);
                    course.setLocation(fields[3].isEmpty() ? null : fields[3]);
                    course.setNfqLevel(fields[4].isEmpty() ? null : Integer.parseInt(fields[4]));
                    course.setDuration(fields[5].isEmpty() ? null : fields[5]);
                    course.setCategories(fields[6].isEmpty() ? null : fields[6]);
                    course.setCourseUrl(fields[7].isEmpty() ? null : fields[7]);
                    course.setDescription(fields[8].isEmpty() ? null : fields[8]);
                    
                    CaoCourse savedCourse = caoCourseRepository.save(course);
                    
                    // Parse points history from the last field
                    if (fields.length > 9 && !fields[9].isEmpty()) {
                        parseAndSavePointsHistory(savedCourse, fields[9]);
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.error("Error loading CAO courses data: ", e);
        }
    }
    
    private void loadMathQuestionsData() {
        try {
            ClassPathResource resource = new ClassPathResource("data/math_questions_db.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                if (fields.length >= 16) {
                    try {
                        MathQuestion question = new MathQuestion();
                        question.setId(Long.parseLong(fields[0]));
                        question.setSubject(fields[1]);
                        question.setYear(Integer.parseInt(fields[2]));
                        question.setPaperType(fields[3]);
                        question.setQuestionNumber(Integer.parseInt(fields[4]));
                        question.setSubPart(fields[5].isEmpty() ? null : fields[5]);
                        question.setQuestionText(fields[6]);
                        question.setFullQuestionText(fields[7].isEmpty() ? null : fields[7]);
                        question.setImages(fields[8].isEmpty() ? null : fields[8]);
                        question.setMarks(fields[9].isEmpty() ? null : Integer.parseInt(fields[9]));
                        
                        // Parse difficulty
                        if (!fields[10].isEmpty()) {
                            try {
                                question.setDifficulty(MathQuestion.Difficulty.valueOf(fields[10].toUpperCase()));
                            } catch (IllegalArgumentException e) {
                                question.setDifficulty(MathQuestion.Difficulty.MEDIUM);
                            }
                        }
                        
                        question.setRawText(fields[11].isEmpty() ? null : fields[11]);
                        question.setPageNumber(fields[12].isEmpty() ? null : Integer.parseInt(fields[12]));
                        question.setPositionInPaper(fields[13].isEmpty() ? null : Integer.parseInt(fields[13]));
                        question.setTextHash(fields[14].isEmpty() ? null : fields[14]);
                        
                        if (fields.length > 15) {
                            question.setCreatedAt(LocalDateTime.parse(fields[15].replace(" ", "T")));
                        }
                        
                        mathQuestionRepository.save(question);
                    } catch (NumberFormatException e) {
                        logger.warn("Skipping invalid math question row: " + line);
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.error("Error loading math questions data: ", e);
        }
    }
    
    private void parseAndSavePointsHistory(CaoCourse course, String pointsHistoryStr) {
        // Parse points history format: "2024(1): 269; 2024(2): 269"
        Pattern pattern = Pattern.compile("(\\d{4})\\((\\d+)\\):\\s*(\\d+)");
        Matcher matcher = pattern.matcher(pointsHistoryStr);
        
        while (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int round = Integer.parseInt(matcher.group(2));
                int points = Integer.parseInt(matcher.group(3));
                
                CaoPointsHistory history = pointsHistoryRepository
                    .findByCaoCourseCaoCodeAndYear(course.getCaoCode(), year)
                    .orElse(new CaoPointsHistory(course, year));
                
                switch (round) {
                    case 1:
                        history.setRound1Points(points);
                        break;
                    case 2:
                        history.setRound2Points(points);
                        break;
                    case 3:
                        history.setRound3Points(points);
                        break;
                }
                
                pointsHistoryRepository.save(history);
            } catch (NumberFormatException e) {
                logger.warn("Invalid points history format: " + matcher.group());
            }
        }
    }
    
    private String[] parseCsvLine(String line) {
        // Simple CSV parser that handles quoted fields
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean doubleQuotesInColumn = false;
        String[] result = new String[20]; // Max expected fields
        int resultIndex = 0;
        
        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                if (!inQuotes) {
                    inQuotes = true;
                    startCollectChar = true;
                } else {
                    if (doubleQuotesInColumn) {
                        current.append(ch);
                        doubleQuotesInColumn = false;
                    } else {
                        inQuotes = false;
                    }
                }
            } else if (ch == ',') {
                if (inQuotes) {
                    current.append(ch);
                } else {
                    if (resultIndex < result.length) {
                        result[resultIndex++] = current.toString();
                    }
                    current = new StringBuilder();
                    startCollectChar = false;
                }
            } else {
                current.append(ch);
                startCollectChar = true;
            }
        }
        
        if (resultIndex < result.length) {
            result[resultIndex++] = current.toString();
        }
        
        // Trim the result array to actual size
        String[] trimmedResult = new String[resultIndex];
        System.arraycopy(result, 0, trimmedResult, 0, resultIndex);
        
        return trimmedResult;
    }
    
    private void createTestUser() {
        try {
            User testUser = new User();
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            testUser.setUsername("testuser");
            testUser.setEmail("test@example.com");
            testUser.setPassword("password123"); // In real app, this would be hashed
            testUser.setRole(User.Role.STUDENT);
            
            userRepository.save(testUser);
            logger.info("Test user created with ID: 1");
        } catch (Exception e) {
            logger.error("Error creating test user: ", e);
        }
    }
    
    private void createSampleAchievements() {
        try {
            Achievement[] achievements = {
                new Achievement("First Steps", "Complete your first study session", "star", Achievement.AchievementType.SESSIONS_COMPLETED, 1),
                new Achievement("Getting Started", "Complete 5 study sessions", "target", Achievement.AchievementType.SESSIONS_COMPLETED, 5),
                new Achievement("Study Warrior", "Complete 25 study sessions", "shield", Achievement.AchievementType.SESSIONS_COMPLETED, 25),
                new Achievement("Study Master", "Complete 100 study sessions", "crown", Achievement.AchievementType.SESSIONS_COMPLETED, 100),
                
                new Achievement("First Hour", "Study for 1 hour total", "clock", Achievement.AchievementType.HOURS_STUDIED, 1),
                new Achievement("Dedicated Student", "Study for 10 hours total", "book", Achievement.AchievementType.HOURS_STUDIED, 10),
                new Achievement("Marathon", "Study for 50 hours total", "trophy", Achievement.AchievementType.HOURS_STUDIED, 50),
                new Achievement("Scholar", "Study for 100 hours total", "graduation-cap", Achievement.AchievementType.HOURS_STUDIED, 100),
                
                new Achievement("Day Two", "Study for 2 days in a row", "flame", Achievement.AchievementType.STREAK_DAYS, 2),
                new Achievement("Week Warrior", "Study for 7 days in a row", "calendar", Achievement.AchievementType.STREAK_DAYS, 7),
                new Achievement("Consistency King", "Study for 30 days in a row", "medal", Achievement.AchievementType.STREAK_DAYS, 30),
                
                new Achievement("Early Bird", "Start a study session before 7 AM", "sunrise", Achievement.AchievementType.EARLY_SESSION, null),
            };
            
            for (Achievement achievement : achievements) {
                achievementRepository.save(achievement);
            }
            
            logger.info("Created {} sample achievements", achievements.length);
        } catch (Exception e) {
            logger.error("Error creating sample achievements: ", e);
        }
    }
}