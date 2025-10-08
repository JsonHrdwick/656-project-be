package com.example.springbootjava.service;

import com.example.springbootjava.entity.*;
import com.example.springbootjava.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatabaseBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final String BACKUP_DIR = "backups";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;

    @Autowired
    private FlashcardStudySessionRepository flashcardStudySessionRepository;

    private final ObjectMapper objectMapper;

    public DatabaseBackupService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Creates a complete backup of the database
     */
    @Transactional(readOnly = true)
    public String createBackup() throws IOException {
        logger.info("Starting database backup process");
        
        // Ensure backup directory exists
        createBackupDirectory();
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String backupFileName = String.format("backup_%s.json", timestamp);
        String backupFilePath = Paths.get(BACKUP_DIR, backupFileName).toString();
        
        try {
            // Create backup data structure
            DatabaseBackupData backupData = new DatabaseBackupData();
            backupData.setBackupTimestamp(LocalDateTime.now());
            backupData.setVersion("1.0");
            
            // Export all entities
            backupData.setUsers(exportUsers());
            backupData.setDocuments(exportDocuments());
            backupData.setFlashcards(exportFlashcards());
            backupData.setQuizzes(exportQuizzes());
            backupData.setQuizQuestions(exportQuizQuestions());
            backupData.setQuizAnswers(exportQuizAnswers());
            backupData.setQuizAttempts(exportQuizAttempts());
            backupData.setQuizAttemptAnswers(exportQuizAttemptAnswers());
            backupData.setFlashcardStudySessions(exportFlashcardStudySessions());
            
            // Write to file
            try (FileWriter writer = new FileWriter(backupFilePath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, backupData);
            }
            
            logger.info("Database backup completed successfully: {}", backupFilePath);
            return backupFilePath;
            
        } catch (Exception e) {
            logger.error("Failed to create database backup", e);
            throw new IOException("Backup creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a backup for a specific user
     */
    @Transactional(readOnly = true)
    public String createUserBackup(Long userId) throws IOException {
        logger.info("Starting user backup process for user ID: {}", userId);
        
        createBackupDirectory();
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String backupFileName = String.format("user_backup_%d_%s.json", userId, timestamp);
        String backupFilePath = Paths.get(BACKUP_DIR, backupFileName).toString();
        
        try {
            UserBackupData backupData = new UserBackupData();
            backupData.setBackupTimestamp(LocalDateTime.now());
            backupData.setVersion("1.0");
            backupData.setUserId(userId);
            
            // Export user-specific data
            backupData.setUser(exportUser(userId));
            backupData.setDocuments(exportUserDocuments(userId));
            backupData.setFlashcards(exportUserFlashcards(userId));
            backupData.setQuizzes(exportUserQuizzes(userId));
            backupData.setQuizQuestions(exportUserQuizQuestions(userId));
            backupData.setQuizAnswers(exportUserQuizAnswers(userId));
            backupData.setQuizAttempts(exportUserQuizAttempts(userId));
            backupData.setQuizAttemptAnswers(exportUserQuizAttemptAnswers(userId));
            backupData.setFlashcardStudySessions(exportUserFlashcardStudySessions(userId));
            
            try (FileWriter writer = new FileWriter(backupFilePath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, backupData);
            }
            
            logger.info("User backup completed successfully: {}", backupFilePath);
            return backupFilePath;
            
        } catch (Exception e) {
            logger.error("Failed to create user backup for user ID: {}", userId, e);
            throw new IOException("User backup creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all available backup files
     */
    public List<BackupFileInfo> listBackups() throws IOException {
        Path backupDir = Paths.get(BACKUP_DIR);
        if (!Files.exists(backupDir)) {
            return new ArrayList<>();
        }
        
        return Files.list(backupDir)
                .filter(path -> path.toString().endsWith(".json"))
                .map(path -> {
                    try {
                        return new BackupFileInfo(
                                path.getFileName().toString(),
                                path.toString(),
                                Files.getLastModifiedTime(path).toInstant(),
                                Files.size(path)
                        );
                    } catch (IOException e) {
                        logger.warn("Failed to get file info for: {}", path, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BackupFileInfo::getLastModified).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Deletes old backup files (keeps only the last N backups)
     */
    public int cleanupOldBackups(int keepCount) throws IOException {
        List<BackupFileInfo> backups = listBackups();
        if (backups.size() <= keepCount) {
            return 0;
        }
        
        List<BackupFileInfo> toDelete = backups.subList(keepCount, backups.size());
        int deletedCount = 0;
        
        for (BackupFileInfo backup : toDelete) {
            try {
                Files.deleteIfExists(Paths.get(backup.getFilePath()));
                deletedCount++;
                logger.info("Deleted old backup: {}", backup.getFileName());
            } catch (IOException e) {
                logger.warn("Failed to delete backup: {}", backup.getFileName(), e);
            }
        }
        
        return deletedCount;
    }

    private void createBackupDirectory() throws IOException {
        Path backupDir = Paths.get(BACKUP_DIR);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
            logger.info("Created backup directory: {}", backupDir);
        }
    }

    // Export methods for all entities
    private List<Map<String, Object>> exportUsers() {
        return userRepository.findAll().stream()
                .map(this::convertUserToMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> exportUser(Long userId) {
        return userRepository.findById(userId)
                .map(this::convertUserToMap)
                .orElse(null);
    }

    private List<Map<String, Object>> exportDocuments() {
        return documentRepository.findAll().stream()
                .map(this::convertDocumentToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserDocuments(Long userId) {
        return documentRepository.findByUserId(userId).stream()
                .map(this::convertDocumentToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportFlashcards() {
        return flashcardRepository.findAll().stream()
                .map(this::convertFlashcardToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserFlashcards(Long userId) {
        return flashcardRepository.findByUserId(userId).stream()
                .map(this::convertFlashcardToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportQuizzes() {
        return quizRepository.findAll().stream()
                .map(this::convertQuizToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserQuizzes(Long userId) {
        return quizRepository.findByUserId(userId).stream()
                .map(this::convertQuizToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportQuizQuestions() {
        return quizQuestionRepository.findAll().stream()
                .map(this::convertQuizQuestionToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserQuizQuestions(Long userId) {
        return quizQuestionRepository.findByQuizUserId(userId).stream()
                .map(this::convertQuizQuestionToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportQuizAnswers() {
        return quizAnswerRepository.findAll().stream()
                .map(this::convertQuizAnswerToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserQuizAnswers(Long userId) {
        return quizAnswerRepository.findByQuestionQuizUserId(userId).stream()
                .map(this::convertQuizAnswerToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportQuizAttempts() {
        return quizAttemptRepository.findAll().stream()
                .map(this::convertQuizAttemptToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserQuizAttempts(Long userId) {
        return quizAttemptRepository.findByUserId(userId).stream()
                .map(this::convertQuizAttemptToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportQuizAttemptAnswers() {
        return quizAttemptAnswerRepository.findAll().stream()
                .map(this::convertQuizAttemptAnswerToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserQuizAttemptAnswers(Long userId) {
        return quizAttemptAnswerRepository.findByAttemptUserId(userId).stream()
                .map(this::convertQuizAttemptAnswerToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportFlashcardStudySessions() {
        return flashcardStudySessionRepository.findAll().stream()
                .map(this::convertFlashcardStudySessionToMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportUserFlashcardStudySessions(Long userId) {
        return flashcardStudySessionRepository.findByFlashcardUserId(userId).stream()
                .map(this::convertFlashcardStudySessionToMap)
                .collect(Collectors.toList());
    }

    // Conversion methods to Map for JSON serialization
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("email", user.getEmail());
        userMap.put("password", user.getPassword());
        userMap.put("role", user.getRole().name());
        userMap.put("enabled", user.getEnabled());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        return userMap;
    }

    private Map<String, Object> convertDocumentToMap(Document document) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("id", document.getId());
        docMap.put("title", document.getTitle());
        docMap.put("description", document.getDescription());
        docMap.put("fileType", document.getFileType());
        docMap.put("fileName", document.getFileName());
        docMap.put("filePath", document.getFilePath());
        docMap.put("fileSize", document.getFileSize());
        docMap.put("content", document.getContent());
        docMap.put("summary", document.getSummary());
        docMap.put("processingStatus", document.getProcessingStatus().name());
        docMap.put("createdAt", document.getCreatedAt());
        docMap.put("updatedAt", document.getUpdatedAt());
        docMap.put("userId", document.getUser().getId());
        return docMap;
    }

    private Map<String, Object> convertFlashcardToMap(Flashcard flashcard) {
        Map<String, Object> cardMap = new HashMap<>();
        cardMap.put("id", flashcard.getId());
        cardMap.put("question", flashcard.getQuestion());
        cardMap.put("answer", flashcard.getAnswer());
        cardMap.put("category", flashcard.getCategory());
        cardMap.put("difficulty", flashcard.getDifficulty().name());
        cardMap.put("createdAt", flashcard.getCreatedAt());
        cardMap.put("updatedAt", flashcard.getUpdatedAt());
        cardMap.put("userId", flashcard.getUser().getId());
        if (flashcard.getDocument() != null) {
            cardMap.put("documentId", flashcard.getDocument().getId());
        }
        return cardMap;
    }

    private Map<String, Object> convertQuizToMap(Quiz quiz) {
        Map<String, Object> quizMap = new HashMap<>();
        quizMap.put("id", quiz.getId());
        quizMap.put("title", quiz.getTitle());
        quizMap.put("description", quiz.getDescription());
        quizMap.put("timeLimitMinutes", quiz.getTimeLimitMinutes());
        quizMap.put("difficulty", quiz.getDifficulty().name());
        quizMap.put("isPublished", quiz.getIsPublished());
        quizMap.put("createdAt", quiz.getCreatedAt());
        quizMap.put("updatedAt", quiz.getUpdatedAt());
        quizMap.put("userId", quiz.getUser().getId());
        if (quiz.getDocument() != null) {
            quizMap.put("documentId", quiz.getDocument().getId());
        }
        return quizMap;
    }

    private Map<String, Object> convertQuizQuestionToMap(QuizQuestion question) {
        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("id", question.getId());
        questionMap.put("questionText", question.getQuestionText());
        questionMap.put("questionType", question.getQuestionType().name());
        questionMap.put("points", question.getPoints());
        questionMap.put("order", question.getOrder());
        questionMap.put("createdAt", question.getCreatedAt());
        questionMap.put("updatedAt", question.getUpdatedAt());
        questionMap.put("quizId", question.getQuiz().getId());
        return questionMap;
    }

    private Map<String, Object> convertQuizAnswerToMap(QuizAnswer answer) {
        Map<String, Object> answerMap = new HashMap<>();
        answerMap.put("id", answer.getId());
        answerMap.put("answerText", answer.getAnswerText());
        answerMap.put("isCorrect", answer.getIsCorrect());
        answerMap.put("order", answer.getOrder());
        answerMap.put("createdAt", answer.getCreatedAt());
        answerMap.put("updatedAt", answer.getUpdatedAt());
        answerMap.put("questionId", answer.getQuestion().getId());
        return answerMap;
    }

    private Map<String, Object> convertQuizAttemptToMap(QuizAttempt attempt) {
        Map<String, Object> attemptMap = new HashMap<>();
        attemptMap.put("id", attempt.getId());
        attemptMap.put("score", attempt.getScore());
        attemptMap.put("maxScore", attempt.getMaxScore());
        attemptMap.put("timeSpentMinutes", attempt.getTimeSpentMinutes());
        attemptMap.put("completedAt", attempt.getCompletedAt());
        attemptMap.put("createdAt", attempt.getCreatedAt());
        attemptMap.put("updatedAt", attempt.getUpdatedAt());
        attemptMap.put("userId", attempt.getUser().getId());
        attemptMap.put("quizId", attempt.getQuiz().getId());
        return attemptMap;
    }

    private Map<String, Object> convertQuizAttemptAnswerToMap(QuizAttemptAnswer attemptAnswer) {
        Map<String, Object> answerMap = new HashMap<>();
        answerMap.put("id", attemptAnswer.getId());
        answerMap.put("selectedAnswerId", attemptAnswer.getSelectedAnswer() != null ? attemptAnswer.getSelectedAnswer().getId() : null);
        answerMap.put("isCorrect", attemptAnswer.getIsCorrect());
        answerMap.put("createdAt", attemptAnswer.getCreatedAt());
        answerMap.put("updatedAt", attemptAnswer.getUpdatedAt());
        answerMap.put("attemptId", attemptAnswer.getAttempt().getId());
        answerMap.put("questionId", attemptAnswer.getQuestion().getId());
        return answerMap;
    }

    private Map<String, Object> convertFlashcardStudySessionToMap(FlashcardStudySession session) {
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("id", session.getId());
        sessionMap.put("score", session.getScore());
        sessionMap.put("timeSpentSeconds", session.getTimeSpentSeconds());
        sessionMap.put("createdAt", session.getCreatedAt());
        sessionMap.put("updatedAt", session.getUpdatedAt());
        sessionMap.put("flashcardId", session.getFlashcard().getId());
        return sessionMap;
    }

    // Data classes for backup structure
    public static class DatabaseBackupData {
        private LocalDateTime backupTimestamp;
        private String version;
        private List<Map<String, Object>> users;
        private List<Map<String, Object>> documents;
        private List<Map<String, Object>> flashcards;
        private List<Map<String, Object>> quizzes;
        private List<Map<String, Object>> quizQuestions;
        private List<Map<String, Object>> quizAnswers;
        private List<Map<String, Object>> quizAttempts;
        private List<Map<String, Object>> quizAttemptAnswers;
        private List<Map<String, Object>> flashcardStudySessions;

        // Getters and setters
        public LocalDateTime getBackupTimestamp() { return backupTimestamp; }
        public void setBackupTimestamp(LocalDateTime backupTimestamp) { this.backupTimestamp = backupTimestamp; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public List<Map<String, Object>> getUsers() { return users; }
        public void setUsers(List<Map<String, Object>> users) { this.users = users; }
        public List<Map<String, Object>> getDocuments() { return documents; }
        public void setDocuments(List<Map<String, Object>> documents) { this.documents = documents; }
        public List<Map<String, Object>> getFlashcards() { return flashcards; }
        public void setFlashcards(List<Map<String, Object>> flashcards) { this.flashcards = flashcards; }
        public List<Map<String, Object>> getQuizzes() { return quizzes; }
        public void setQuizzes(List<Map<String, Object>> quizzes) { this.quizzes = quizzes; }
        public List<Map<String, Object>> getQuizQuestions() { return quizQuestions; }
        public void setQuizQuestions(List<Map<String, Object>> quizQuestions) { this.quizQuestions = quizQuestions; }
        public List<Map<String, Object>> getQuizAnswers() { return quizAnswers; }
        public void setQuizAnswers(List<Map<String, Object>> quizAnswers) { this.quizAnswers = quizAnswers; }
        public List<Map<String, Object>> getQuizAttempts() { return quizAttempts; }
        public void setQuizAttempts(List<Map<String, Object>> quizAttempts) { this.quizAttempts = quizAttempts; }
        public List<Map<String, Object>> getQuizAttemptAnswers() { return quizAttemptAnswers; }
        public void setQuizAttemptAnswers(List<Map<String, Object>> quizAttemptAnswers) { this.quizAttemptAnswers = quizAttemptAnswers; }
        public List<Map<String, Object>> getFlashcardStudySessions() { return flashcardStudySessions; }
        public void setFlashcardStudySessions(List<Map<String, Object>> flashcardStudySessions) { this.flashcardStudySessions = flashcardStudySessions; }
    }

    public static class UserBackupData {
        private LocalDateTime backupTimestamp;
        private String version;
        private Long userId;
        private Map<String, Object> user;
        private List<Map<String, Object>> documents;
        private List<Map<String, Object>> flashcards;
        private List<Map<String, Object>> quizzes;
        private List<Map<String, Object>> quizQuestions;
        private List<Map<String, Object>> quizAnswers;
        private List<Map<String, Object>> quizAttempts;
        private List<Map<String, Object>> quizAttemptAnswers;
        private List<Map<String, Object>> flashcardStudySessions;

        // Getters and setters
        public LocalDateTime getBackupTimestamp() { return backupTimestamp; }
        public void setBackupTimestamp(LocalDateTime backupTimestamp) { this.backupTimestamp = backupTimestamp; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Map<String, Object> getUser() { return user; }
        public void setUser(Map<String, Object> user) { this.user = user; }
        public List<Map<String, Object>> getDocuments() { return documents; }
        public void setDocuments(List<Map<String, Object>> documents) { this.documents = documents; }
        public List<Map<String, Object>> getFlashcards() { return flashcards; }
        public void setFlashcards(List<Map<String, Object>> flashcards) { this.flashcards = flashcards; }
        public List<Map<String, Object>> getQuizzes() { return quizzes; }
        public void setQuizzes(List<Map<String, Object>> quizzes) { this.quizzes = quizzes; }
        public List<Map<String, Object>> getQuizQuestions() { return quizQuestions; }
        public void setQuizQuestions(List<Map<String, Object>> quizQuestions) { this.quizQuestions = quizQuestions; }
        public List<Map<String, Object>> getQuizAnswers() { return quizAnswers; }
        public void setQuizAnswers(List<Map<String, Object>> quizAnswers) { this.quizAnswers = quizAnswers; }
        public List<Map<String, Object>> getQuizAttempts() { return quizAttempts; }
        public void setQuizAttempts(List<Map<String, Object>> quizAttempts) { this.quizAttempts = quizAttempts; }
        public List<Map<String, Object>> getQuizAttemptAnswers() { return quizAttemptAnswers; }
        public void setQuizAttemptAnswers(List<Map<String, Object>> quizAttemptAnswers) { this.quizAttemptAnswers = quizAttemptAnswers; }
        public List<Map<String, Object>> getFlashcardStudySessions() { return flashcardStudySessions; }
        public void setFlashcardStudySessions(List<Map<String, Object>> flashcardStudySessions) { this.flashcardStudySessions = flashcardStudySessions; }
    }

    public static class BackupFileInfo {
        private final String fileName;
        private final String filePath;
        private final java.time.Instant lastModified;
        private final long size;

        public BackupFileInfo(String fileName, String filePath, java.time.Instant lastModified, long size) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.lastModified = lastModified;
            this.size = size;
        }

        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public java.time.Instant getLastModified() { return lastModified; }
        public long getSize() { return size; }
    }
}
