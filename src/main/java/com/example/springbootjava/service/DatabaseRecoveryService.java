package com.example.springbootjava.service;

import com.example.springbootjava.entity.*;
import com.example.springbootjava.repository.*;
import com.example.springbootjava.service.DatabaseBackupService.DatabaseBackupData;
import com.example.springbootjava.service.DatabaseBackupService.UserBackupData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatabaseRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseRecoveryService.class);

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper;

    public DatabaseRecoveryService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Recovers the entire database from a backup file
     */
    @Transactional
    public RecoveryResult recoverFromBackup(String backupFilePath) throws IOException {
        logger.info("Starting database recovery from: {}", backupFilePath);
        
        try {
            // Read and parse backup file
            String backupContent = Files.readString(Paths.get(backupFilePath));
            DatabaseBackupData backupData = objectMapper.readValue(backupContent, DatabaseBackupData.class);
            
            RecoveryResult result = new RecoveryResult();
            result.setBackupTimestamp(backupData.getBackupTimestamp());
            result.setBackupVersion(backupData.getVersion());
            result.setRecoveryTimestamp(LocalDateTime.now());
            
            // Clear existing data (optional - can be configured)
            clearExistingData();
            // Reset user id sequence to a known baseline (PostgreSQL). Safe if table is empty after clear.
            try { userRepository.resetUserIdSequenceToStart(); } catch (Exception ignored) {}
            
            // Restore data in dependency order
            Map<Long, Long> userIdMapping = restoreUsers(backupData.getUsers(), result);
            Map<Long, Long> documentIdMapping = restoreDocuments(backupData.getDocuments(), userIdMapping, result);
            Map<Long, Long> flashcardIdMapping = restoreFlashcards(backupData.getFlashcards(), userIdMapping, documentIdMapping, result);
            Map<Long, Long> quizIdMapping = restoreQuizzes(backupData.getQuizzes(), userIdMapping, documentIdMapping, result);
            Map<Long, Long> questionIdMapping = restoreQuizQuestions(backupData.getQuizQuestions(), quizIdMapping, result);
            Map<Long, Long> answerIdMapping = restoreQuizAnswers(backupData.getQuizAnswers(), questionIdMapping, result);
            Map<Long, Long> attemptIdMapping = restoreQuizAttempts(backupData.getQuizAttempts(), userIdMapping, quizIdMapping, result);
            restoreQuizAttemptAnswers(backupData.getQuizAttemptAnswers(), attemptIdMapping, answerIdMapping, questionIdMapping, result);
            restoreFlashcardStudySessions(backupData.getFlashcardStudySessions(), flashcardIdMapping, result);
            
            logger.info("Database recovery completed successfully");
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to recover database from backup", e);
            throw new IOException("Recovery failed: " + e.getMessage(), e);
        }
    }

    /**
     * Recovers user data from a user-specific backup file
     */
    @Transactional
    public RecoveryResult recoverUserFromBackup(String backupFilePath, Long targetUserId) throws IOException {
        logger.info("Starting user recovery from: {} for user ID: {}", backupFilePath, targetUserId);
        
        try {
            String backupContent = Files.readString(Paths.get(backupFilePath));
            UserBackupData backupData = objectMapper.readValue(backupContent, UserBackupData.class);
            
            RecoveryResult result = new RecoveryResult();
            result.setBackupTimestamp(backupData.getBackupTimestamp());
            result.setBackupVersion(backupData.getVersion());
            result.setRecoveryTimestamp(LocalDateTime.now());
            
            // Clear existing user data
            clearUserData(targetUserId);
            
            // Restore user data
            Map<Long, Long> userIdMapping = restoreUser(backupData.getUser(), targetUserId, result);
            Map<Long, Long> documentIdMapping = restoreUserDocuments(backupData.getDocuments(), userIdMapping, result);
            Map<Long, Long> flashcardIdMapping = restoreUserFlashcards(backupData.getFlashcards(), userIdMapping, documentIdMapping, result);
            Map<Long, Long> quizIdMapping = restoreUserQuizzes(backupData.getQuizzes(), userIdMapping, documentIdMapping, result);
            Map<Long, Long> questionIdMapping = restoreUserQuizQuestions(backupData.getQuizQuestions(), quizIdMapping, result);
            Map<Long, Long> answerIdMapping = restoreUserQuizAnswers(backupData.getQuizAnswers(), questionIdMapping, result);
            Map<Long, Long> attemptIdMapping = restoreUserQuizAttempts(backupData.getQuizAttempts(), userIdMapping, quizIdMapping, result);
            restoreUserQuizAttemptAnswers(backupData.getQuizAttemptAnswers(), attemptIdMapping, answerIdMapping, questionIdMapping, result);
            restoreUserFlashcardStudySessions(backupData.getFlashcardStudySessions(), flashcardIdMapping, result);
            
            logger.info("User recovery completed successfully");
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to recover user data from backup", e);
            throw new IOException("User recovery failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a backup file before recovery
     */
    public ValidationResult validateBackup(String backupFilePath) throws IOException {
        logger.info("Validating backup file: {}", backupFilePath);
        
        try {
            String backupContent = Files.readString(Paths.get(backupFilePath));
            DatabaseBackupData backupData = objectMapper.readValue(backupContent, DatabaseBackupData.class);
            
            ValidationResult result = new ValidationResult();
            result.setValid(true);
            result.setBackupTimestamp(backupData.getBackupTimestamp());
            result.setBackupVersion(backupData.getVersion());
            
            // Validate data integrity
            validateUsers(backupData.getUsers(), result);
            validateDocuments(backupData.getDocuments(), result);
            validateFlashcards(backupData.getFlashcards(), result);
            validateQuizzes(backupData.getQuizzes(), result);
            validateQuizQuestions(backupData.getQuizQuestions(), result);
            validateQuizAnswers(backupData.getQuizAnswers(), result);
            validateQuizAttempts(backupData.getQuizAttempts(), result);
            validateQuizAttemptAnswers(backupData.getQuizAttemptAnswers(), result);
            validateFlashcardStudySessions(backupData.getFlashcardStudySessions(), result);
            
            logger.info("Backup validation completed. Valid: {}", result.isValid());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to validate backup file", e);
            ValidationResult result = new ValidationResult();
            result.setValid(false);
            result.addError("Failed to parse backup file: " + e.getMessage());
            return result;
        }
    }

    private void clearExistingData() {
        logger.info("Clearing existing data");
        quizAttemptAnswerRepository.deleteAll();
        quizAttemptRepository.deleteAll();
        quizAnswerRepository.deleteAll();
        quizQuestionRepository.deleteAll();
        quizRepository.deleteAll();
        flashcardStudySessionRepository.deleteAll();
        flashcardRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void clearUserData(Long userId) {
        logger.info("Clearing data for user ID: {}", userId);
        quizAttemptAnswerRepository.deleteByAttemptUserId(userId);
        quizAttemptRepository.deleteByUserId(userId);
        quizAnswerRepository.deleteByQuestionQuizUserId(userId);
        quizQuestionRepository.deleteByQuizUserId(userId);
        quizRepository.deleteByUserId(userId);
        flashcardStudySessionRepository.deleteByFlashcardUserId(userId);
        flashcardRepository.deleteByUserId(userId);
        documentRepository.deleteByUserId(userId);
    }

    // Restore methods
    private Map<Long, Long> restoreUsers(List<Map<String, Object>> users, RecoveryResult result) {
        Map<Long, Long> idMapping = new HashMap<>();
        
        for (Map<String, Object> userData : users) {
            try {
                Long originalId = toLong(userData.get("id"));
                User user = new User();
                user.setFirstName((String) userData.get("firstName"));
                user.setLastName((String) userData.get("lastName"));
                user.setEmail((String) userData.get("email"));
                user.setPassword(passwordEncoder.encode((String) userData.get("password")));
                user.setRole(User.Role.valueOf((String) userData.get("role")));
                user.setEnabled((Boolean) userData.get("enabled"));
                user.setCreatedAt(parseDateTime(userData.get("createdAt")));
                user.setUpdatedAt(parseDateTime(userData.get("updatedAt")));
                
                // Force to original id if possible (PostgreSQL). Otherwise fall back to save
                if (originalId != null) {
                    try {
                        userRepository.insertWithId(
                            originalId,
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getPassword(),
                            user.getRole().name(),
                            user.getEnabled(),
                            user.getCreatedAt(),
                            user.getUpdatedAt()
                        );
                        idMapping.put(originalId, originalId);
                    } catch (Exception ex) {
                        User savedUser = userRepository.save(user);
                        idMapping.put(originalId, savedUser.getId());
                    }
                } else {
                    User savedUser = userRepository.save(user);
                    idMapping.put(savedUser.getId(), savedUser.getId());
                }
            // Ensure sequence is aligned to max(id) (no-op for non-Postgres)
            try { userRepository.syncUserIdSequence(); } catch (Exception ignored) {}
            result.incrementUsersRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore user: {}", userData.get("email"), e);
                result.addError("Failed to restore user: " + userData.get("email"));
            }
        }
        
        return idMapping;
    }

	private Map<Long, Long> restoreUser(Map<String, Object> userData, Long targetUserId, RecoveryResult result) {
        Map<Long, Long> idMapping = new HashMap<>();
        
        try {
			// Update existing target user in place to preserve its ID. If not present, insert with the target ID.
			User user = userRepository.findById(targetUserId).orElse(null);
			if (user == null) {
				user = new User();
				user.setId(targetUserId);
			}
            user.setFirstName((String) userData.get("firstName"));
            user.setLastName((String) userData.get("lastName"));
            user.setEmail((String) userData.get("email"));
            user.setPassword(passwordEncoder.encode((String) userData.get("password")));
            user.setRole(User.Role.valueOf((String) userData.get("role")));
            user.setEnabled((Boolean) userData.get("enabled"));
            user.setCreatedAt(parseDateTime(userData.get("createdAt")));
            user.setUpdatedAt(parseDateTime(userData.get("updatedAt")));
            
            User savedUser = userRepository.save(user);
			idMapping.put(toLong(userData.get("id")), savedUser.getId());
            result.incrementUsersRestored();
            
        } catch (Exception e) {
            logger.warn("Failed to restore user: {}", userData.get("email"), e);
            result.addError("Failed to restore user: " + userData.get("email"));
        }
        
        return idMapping;
    }

    private Map<Long, Long> restoreDocuments(List<Map<String, Object>> documents, Map<Long, Long> userIdMapping, RecoveryResult result) {
        Map<Long, Long> idMapping = new HashMap<>();
        
        for (Map<String, Object> docData : documents) {
            try {
                Document document = new Document();
                document.setTitle((String) docData.get("title"));
                document.setDescription((String) docData.get("description"));
                document.setFileType((String) docData.get("fileType"));
                document.setFileName((String) docData.get("fileName"));
                document.setFilePath((String) docData.get("filePath"));
                document.setFileSize(toLong(docData.get("fileSize")));
                document.setContent((String) docData.get("content"));
                document.setSummary((String) docData.get("summary"));
                document.setProcessingStatus(Document.ProcessingStatus.valueOf((String) docData.get("processingStatus")));
                document.setCreatedAt(parseDateTime(docData.get("createdAt")));
                document.setUpdatedAt(parseDateTime(docData.get("updatedAt")));
                
                Long userId = userIdMapping.get(toLong(docData.get("userId")));
                if (userId != null) {
                    User user = new User();
                    user.setId(userId);
                    document.setUser(user);
                }
                
                Document savedDoc = documentRepository.save(document);
                idMapping.put(toLong(docData.get("id")), savedDoc.getId());
                result.incrementDocumentsRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore document: {}", docData.get("title"), e);
                result.addError("Failed to restore document: " + docData.get("title"));
            }
        }
        
        return idMapping;
    }

    private Map<Long, Long> restoreUserDocuments(List<Map<String, Object>> documents, Map<Long, Long> userIdMapping, RecoveryResult result) {
        return restoreDocuments(documents, userIdMapping, result);
    }

    private Map<Long, Long> restoreFlashcards(List<Map<String, Object>> flashcards, Map<Long, Long> userIdMapping, Map<Long, Long> documentIdMapping, RecoveryResult result) {
        Map<Long, Long> idMapping = new HashMap<>();
        
        for (Map<String, Object> cardData : flashcards) {
            try {
                Flashcard flashcard = new Flashcard();
                flashcard.setQuestion((String) cardData.get("question"));
                flashcard.setAnswer((String) cardData.get("answer"));
                flashcard.setCategory((String) cardData.get("category"));
                flashcard.setDifficulty(Flashcard.Difficulty.valueOf((String) cardData.get("difficulty")));
                flashcard.setCreatedAt(parseDateTime(cardData.get("createdAt")));
                flashcard.setUpdatedAt(parseDateTime(cardData.get("updatedAt")));
                
                Long userId = userIdMapping.get(toLong(cardData.get("userId")));
                if (userId != null) {
                    User user = new User();
                    user.setId(userId);
                    flashcard.setUser(user);
                }
                
                Long documentId = toLong(cardData.get("documentId"));
                if (documentId != null && documentIdMapping.containsKey(documentId)) {
                    Document document = new Document();
                    document.setId(documentIdMapping.get(documentId));
                    flashcard.setDocument(document);
                }
                
                Flashcard savedCard = flashcardRepository.save(flashcard);
                idMapping.put(toLong(cardData.get("id")), savedCard.getId());
                result.incrementFlashcardsRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore flashcard", e);
                result.addError("Failed to restore flashcard");
            }
        }
        
        return idMapping;
    }

    private Map<Long, Long> restoreUserFlashcards(List<Map<String, Object>> flashcards, Map<Long, Long> userIdMapping, Map<Long, Long> documentIdMapping, RecoveryResult result) {
        return restoreFlashcards(flashcards, userIdMapping, documentIdMapping, result);
    }

    private Map<Long, Long> restoreQuizzes(List<Map<String, Object>> quizzes, Map<Long, Long> userIdMapping, Map<Long, Long> documentIdMapping, RecoveryResult result) {
        Map<Long, Long> idMapping = new HashMap<>();
        
        for (Map<String, Object> quizData : quizzes) {
            try {
                Quiz quiz = new Quiz();
                quiz.setTitle((String) quizData.get("title"));
                quiz.setDescription((String) quizData.get("description"));
                quiz.setTimeLimitMinutes((Integer) quizData.get("timeLimitMinutes"));
                quiz.setDifficulty(Quiz.Difficulty.valueOf((String) quizData.get("difficulty")));
                quiz.setIsPublished((Boolean) quizData.get("isPublished"));
                quiz.setCreatedAt(parseDateTime(quizData.get("createdAt")));
                quiz.setUpdatedAt(parseDateTime(quizData.get("updatedAt")));
                
                Long userId = userIdMapping.get(toLong(quizData.get("userId")));
                if (userId != null) {
                    User user = new User();
                    user.setId(userId);
                    quiz.setUser(user);
                }
                
                Long documentId = toLong(quizData.get("documentId"));
                if (documentId != null && documentIdMapping.containsKey(documentId)) {
                    Document document = new Document();
                    document.setId(documentIdMapping.get(documentId));
                    quiz.setDocument(document);
                }
                
                Quiz savedQuiz = quizRepository.save(quiz);
                idMapping.put(toLong(quizData.get("id")), savedQuiz.getId());
                result.incrementQuizzesRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore quiz: {}", quizData.get("title"), e);
                result.addError("Failed to restore quiz: " + quizData.get("title"));
            }
        }
        
        return idMapping;
    }

    private Map<Long, Long> restoreUserQuizzes(List<Map<String, Object>> quizzes, Map<Long, Long> userIdMapping, Map<Long, Long> documentIdMapping, RecoveryResult result) {
        return restoreQuizzes(quizzes, userIdMapping, documentIdMapping, result);
    }

    private Map<Long, Long> restoreQuizQuestions(List<Map<String, Object>> questions, Map<Long, Long> quizIdMapping, RecoveryResult result) {
        Map<Long, Long> idMapping = new HashMap<>();
        
        for (Map<String, Object> questionData : questions) {
            try {
                QuizQuestion question = new QuizQuestion();
                question.setQuestionText((String) questionData.get("questionText"));
                question.setQuestionType(QuizQuestion.QuestionType.valueOf((String) questionData.get("questionType")));
                question.setPoints((Integer) questionData.get("points"));
                question.setOrder((Integer) questionData.get("order"));
                question.setCreatedAt(parseDateTime(questionData.get("createdAt")));
                question.setUpdatedAt(parseDateTime(questionData.get("updatedAt")));
                
                Long quizId = quizIdMapping.get(toLong(questionData.get("quizId")));
                if (quizId != null) {
                    Quiz quiz = new Quiz();
                    quiz.setId(quizId);
                    question.setQuiz(quiz);
                }
                
                QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                idMapping.put(toLong(questionData.get("id")), savedQuestion.getId());
                result.incrementQuizQuestionsRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore quiz question", e);
                result.addError("Failed to restore quiz question");
            }
        }
        
        return idMapping;
    }

    private Map<Long, Long> restoreUserQuizQuestions(List<Map<String, Object>> questions, Map<Long, Long> quizIdMapping, RecoveryResult result) {
        return restoreQuizQuestions(questions, quizIdMapping, result);
    }

    private Map<Long, Long> restoreQuizAnswers(List<Map<String, Object>> answers, Map<Long, Long> questionIdMapping, RecoveryResult result) {
        Map<Long, Long> idMapping = new HashMap<>();
        
        for (Map<String, Object> answerData : answers) {
            try {
                QuizAnswer answer = new QuizAnswer();
                answer.setAnswerText((String) answerData.get("answerText"));
                answer.setIsCorrect((Boolean) answerData.get("isCorrect"));
                answer.setOrder((Integer) answerData.get("order"));
                answer.setCreatedAt(parseDateTime(answerData.get("createdAt")));
                answer.setUpdatedAt(parseDateTime(answerData.get("updatedAt")));
                
                Long questionId = questionIdMapping.get(toLong(answerData.get("questionId")));
                if (questionId != null) {
                    QuizQuestion question = new QuizQuestion();
                    question.setId(questionId);
                    answer.setQuestion(question);
                }
                
                QuizAnswer savedAnswer = quizAnswerRepository.save(answer);
                idMapping.put(toLong(answerData.get("id")), savedAnswer.getId());
                result.incrementQuizAnswersRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore quiz answer", e);
                result.addError("Failed to restore quiz answer");
            }
        }
        
        return idMapping;
    }

    private Map<Long, Long> restoreUserQuizAnswers(List<Map<String, Object>> answers, Map<Long, Long> questionIdMapping, RecoveryResult result) {
        return restoreQuizAnswers(answers, questionIdMapping, result);
    }

    private Map<Long, Long> restoreQuizAttempts(List<Map<String, Object>> attempts, Map<Long, Long> userIdMapping, Map<Long, Long> quizIdMapping, RecoveryResult result) {
        Map<Long, Long> idMapping = new HashMap<>();
        
        for (Map<String, Object> attemptData : attempts) {
            try {
                QuizAttempt attempt = new QuizAttempt();
                attempt.setScore((Double) attemptData.get("score"));
                attempt.setMaxScore((Double) attemptData.get("maxScore"));
                attempt.setTimeSpentMinutes((Integer) attemptData.get("timeSpentMinutes"));
                attempt.setCompletedAt(parseDateTime(attemptData.get("completedAt")));
                attempt.setCreatedAt(parseDateTime(attemptData.get("createdAt")));
                attempt.setUpdatedAt(parseDateTime(attemptData.get("updatedAt")));
                
                Long userId = userIdMapping.get(toLong(attemptData.get("userId")));
                if (userId != null) {
                    User user = new User();
                    user.setId(userId);
                    attempt.setUser(user);
                }
                
                Long quizId = quizIdMapping.get(toLong(attemptData.get("quizId")));
                if (quizId != null) {
                    Quiz quiz = new Quiz();
                    quiz.setId(quizId);
                    attempt.setQuiz(quiz);
                }
                
                QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
                idMapping.put(toLong(attemptData.get("id")), savedAttempt.getId());
                result.incrementQuizAttemptsRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore quiz attempt", e);
                result.addError("Failed to restore quiz attempt");
            }
        }
        
        return idMapping;
    }

    private Map<Long, Long> restoreUserQuizAttempts(List<Map<String, Object>> attempts, Map<Long, Long> userIdMapping, Map<Long, Long> quizIdMapping, RecoveryResult result) {
        return restoreQuizAttempts(attempts, userIdMapping, quizIdMapping, result);
    }

    private void restoreQuizAttemptAnswers(List<Map<String, Object>> attemptAnswers, Map<Long, Long> attemptIdMapping, Map<Long, Long> answerIdMapping, Map<Long, Long> questionIdMapping, RecoveryResult result) {
        for (Map<String, Object> answerData : attemptAnswers) {
            try {
                QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
                attemptAnswer.setIsCorrect((Boolean) answerData.get("isCorrect"));
                attemptAnswer.setCreatedAt(parseDateTime(answerData.get("createdAt")));
                attemptAnswer.setUpdatedAt(parseDateTime(answerData.get("updatedAt")));
                
                Long attemptId = attemptIdMapping.get(toLong(answerData.get("attemptId")));
                if (attemptId != null) {
                    QuizAttempt attempt = new QuizAttempt();
                    attempt.setId(attemptId);
                    attemptAnswer.setAttempt(attempt);
                }
                
                Long questionId = questionIdMapping.get(toLong(answerData.get("questionId")));
                if (questionId != null) {
                    QuizQuestion question = new QuizQuestion();
                    question.setId(questionId);
                    attemptAnswer.setQuestion(question);
                }
                
                Long selectedAnswerId = toLong(answerData.get("selectedAnswerId"));
                if (selectedAnswerId != null && answerIdMapping.containsKey(selectedAnswerId)) {
                    QuizAnswer answer = new QuizAnswer();
                    answer.setId(answerIdMapping.get(selectedAnswerId));
                    attemptAnswer.setSelectedAnswer(answer);
                }
                
                quizAttemptAnswerRepository.save(attemptAnswer);
                result.incrementQuizAttemptAnswersRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore quiz attempt answer", e);
                result.addError("Failed to restore quiz attempt answer");
            }
        }
    }

    private void restoreUserQuizAttemptAnswers(List<Map<String, Object>> attemptAnswers, Map<Long, Long> attemptIdMapping, Map<Long, Long> answerIdMapping, Map<Long, Long> questionIdMapping, RecoveryResult result) {
        restoreQuizAttemptAnswers(attemptAnswers, attemptIdMapping, answerIdMapping, questionIdMapping, result);
    }

    private void restoreFlashcardStudySessions(List<Map<String, Object>> sessions, Map<Long, Long> flashcardIdMapping, RecoveryResult result) {
        for (Map<String, Object> sessionData : sessions) {
            try {
                FlashcardStudySession session = new FlashcardStudySession();
                session.setScore((Double) sessionData.get("score"));
                session.setTimeSpentSeconds((Integer) sessionData.get("timeSpentSeconds"));
                session.setCreatedAt(parseDateTime(sessionData.get("createdAt")));
                session.setUpdatedAt(parseDateTime(sessionData.get("updatedAt")));
                
                Long flashcardId = flashcardIdMapping.get(toLong(sessionData.get("flashcardId")));
                if (flashcardId != null) {
                    Flashcard flashcard = new Flashcard();
                    flashcard.setId(flashcardId);
                    session.setFlashcard(flashcard);
                }
                
                flashcardStudySessionRepository.save(session);
                result.incrementFlashcardStudySessionsRestored();
                
            } catch (Exception e) {
                logger.warn("Failed to restore flashcard study session", e);
                result.addError("Failed to restore flashcard study session");
            }
        }
    }

    private void restoreUserFlashcardStudySessions(List<Map<String, Object>> sessions, Map<Long, Long> flashcardIdMapping, RecoveryResult result) {
        restoreFlashcardStudySessions(sessions, flashcardIdMapping, result);
    }

    // Validation methods
    private void validateUsers(List<Map<String, Object>> users, ValidationResult result) {
        if (users == null) return;
        
        for (Map<String, Object> user : users) {
            if (user.get("email") == null || user.get("password") == null) {
                result.addError("User missing required fields: email or password");
            }
        }
    }

    private void validateDocuments(List<Map<String, Object>> documents, ValidationResult result) {
        if (documents == null) return;
        
        for (Map<String, Object> doc : documents) {
            if (doc.get("title") == null || doc.get("userId") == null) {
                result.addError("Document missing required fields: title or userId");
            }
        }
    }

    private void validateFlashcards(List<Map<String, Object>> flashcards, ValidationResult result) {
        if (flashcards == null) return;
        
        for (Map<String, Object> card : flashcards) {
            if (card.get("question") == null || card.get("answer") == null || card.get("userId") == null) {
                result.addError("Flashcard missing required fields: question, answer, or userId");
            }
        }
    }

    private void validateQuizzes(List<Map<String, Object>> quizzes, ValidationResult result) {
        if (quizzes == null) return;
        
        for (Map<String, Object> quiz : quizzes) {
            if (quiz.get("title") == null || quiz.get("userId") == null) {
                result.addError("Quiz missing required fields: title or userId");
            }
        }
    }

    private void validateQuizQuestions(List<Map<String, Object>> questions, ValidationResult result) {
        if (questions == null) return;
        
        for (Map<String, Object> question : questions) {
            if (question.get("questionText") == null || question.get("quizId") == null) {
                result.addError("Quiz question missing required fields: questionText or quizId");
            }
        }
    }

    private void validateQuizAnswers(List<Map<String, Object>> answers, ValidationResult result) {
        if (answers == null) return;
        
        for (Map<String, Object> answer : answers) {
            if (answer.get("answerText") == null || answer.get("questionId") == null) {
                result.addError("Quiz answer missing required fields: answerText or questionId");
            }
        }
    }

    private void validateQuizAttempts(List<Map<String, Object>> attempts, ValidationResult result) {
        if (attempts == null) return;
        
        for (Map<String, Object> attempt : attempts) {
            if (attempt.get("userId") == null || attempt.get("quizId") == null) {
                result.addError("Quiz attempt missing required fields: userId or quizId");
            }
        }
    }

    private void validateQuizAttemptAnswers(List<Map<String, Object>> attemptAnswers, ValidationResult result) {
        if (attemptAnswers == null) return;
        
        for (Map<String, Object> answer : attemptAnswers) {
            if (answer.get("attemptId") == null || answer.get("questionId") == null) {
                result.addError("Quiz attempt answer missing required fields: attemptId or questionId");
            }
        }
    }

    private void validateFlashcardStudySessions(List<Map<String, Object>> sessions, ValidationResult result) {
        if (sessions == null) return;
        
        for (Map<String, Object> session : sessions) {
            if (session.get("flashcardId") == null) {
                result.addError("Flashcard study session missing required field: flashcardId");
            }
        }
    }

    private LocalDateTime parseDateTime(Object dateTime) {
        if (dateTime == null) return null;
        if (dateTime instanceof String) {
            return LocalDateTime.parse((String) dateTime);
        }
        return (LocalDateTime) dateTime;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        throw new IllegalArgumentException("Expected numeric id but got: " + value.getClass());
    }

    // Data classes
    public static class RecoveryResult {
        private LocalDateTime backupTimestamp;
        private String backupVersion;
        private LocalDateTime recoveryTimestamp;
        private int usersRestored = 0;
        private int documentsRestored = 0;
        private int flashcardsRestored = 0;
        private int quizzesRestored = 0;
        private int quizQuestionsRestored = 0;
        private int quizAnswersRestored = 0;
        private int quizAttemptsRestored = 0;
        private int quizAttemptAnswersRestored = 0;
        private int flashcardStudySessionsRestored = 0;
        private List<String> errors = new ArrayList<>();

        // Getters and setters
        public LocalDateTime getBackupTimestamp() { return backupTimestamp; }
        public void setBackupTimestamp(LocalDateTime backupTimestamp) { this.backupTimestamp = backupTimestamp; }
        public String getBackupVersion() { return backupVersion; }
        public void setBackupVersion(String backupVersion) { this.backupVersion = backupVersion; }
        public LocalDateTime getRecoveryTimestamp() { return recoveryTimestamp; }
        public void setRecoveryTimestamp(LocalDateTime recoveryTimestamp) { this.recoveryTimestamp = recoveryTimestamp; }
        public int getUsersRestored() { return usersRestored; }
        public void incrementUsersRestored() { this.usersRestored++; }
        public int getDocumentsRestored() { return documentsRestored; }
        public void incrementDocumentsRestored() { this.documentsRestored++; }
        public int getFlashcardsRestored() { return flashcardsRestored; }
        public void incrementFlashcardsRestored() { this.flashcardsRestored++; }
        public int getQuizzesRestored() { return quizzesRestored; }
        public void incrementQuizzesRestored() { this.quizzesRestored++; }
        public int getQuizQuestionsRestored() { return quizQuestionsRestored; }
        public void incrementQuizQuestionsRestored() { this.quizQuestionsRestored++; }
        public int getQuizAnswersRestored() { return quizAnswersRestored; }
        public void incrementQuizAnswersRestored() { this.quizAnswersRestored++; }
        public int getQuizAttemptsRestored() { return quizAttemptsRestored; }
        public void incrementQuizAttemptsRestored() { this.quizAttemptsRestored++; }
        public int getQuizAttemptAnswersRestored() { return quizAttemptAnswersRestored; }
        public void incrementQuizAttemptAnswersRestored() { this.quizAttemptAnswersRestored++; }
        public int getFlashcardStudySessionsRestored() { return flashcardStudySessionsRestored; }
        public void incrementFlashcardStudySessionsRestored() { this.flashcardStudySessionsRestored++; }
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
    }

    public static class ValidationResult {
        private boolean valid = true;
        private LocalDateTime backupTimestamp;
        private String backupVersion;
        private List<String> errors = new ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public LocalDateTime getBackupTimestamp() { return backupTimestamp; }
        public void setBackupTimestamp(LocalDateTime backupTimestamp) { this.backupTimestamp = backupTimestamp; }
        public String getBackupVersion() { return backupVersion; }
        public void setBackupVersion(String backupVersion) { this.backupVersion = backupVersion; }
        public List<String> getErrors() { return errors; }
        public void addError(String error) { 
            this.errors.add(error);
            this.valid = false;
        }
    }
}
