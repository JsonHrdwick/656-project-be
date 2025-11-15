package com.example.springbootjava.service;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.Quiz;
import com.example.springbootjava.entity.QuizQuestion;
import com.example.springbootjava.entity.QuizAnswer;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.QuizRepository;
import com.example.springbootjava.repository.QuizQuestionRepository;
import com.example.springbootjava.repository.QuizAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
@Transactional
public class QuizService {
    
    @Autowired
    private QuizRepository quizRepository;
    
    @Autowired
    private QuizQuestionRepository quizQuestionRepository;
    
    @Autowired
    private QuizAnswerRepository quizAnswerRepository;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private DocumentContentExtractor contentExtractor;
    
    public List<Quiz> getUserQuizzes(User user) {
        return quizRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Page<Quiz> getUserQuizzes(User user, Pageable pageable) {
        return quizRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    public List<Quiz> getPublishedQuizzes(User user) {
        return quizRepository.findPublishedByUser(user);
    }
    
    public List<Quiz> getQuizzesByDifficulty(User user, Quiz.Difficulty difficulty) {
        return quizRepository.findByUserAndDifficulty(user, difficulty);
    }
    
    public List<Quiz> searchQuizzes(User user, String searchTerm) {
        return quizRepository.findByUserAndSearchTerm(user, searchTerm);
    }
    
    public long getQuizCount(User user) {
        return quizRepository.countByUser(user);
    }
    
    public long getPublishedQuizCount(User user) {
        return quizRepository.countPublishedByUser(user);
    }
    
    public Quiz createQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }
    
    public Optional<Quiz> getQuizById(Long id) {
        return quizRepository.findById(id);
    }
    
    public Quiz updateQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }
    
    public void deleteQuiz(Long id) {
        quizRepository.deleteById(id);
    }
    
    public Quiz generateQuizFromDocument(Document document, User user, int numberOfQuestions) {
        // Validate document is processed
        if (document.getProcessingStatus() != Document.ProcessingStatus.COMPLETED) {
            throw new IllegalStateException("Document must be processed before generating quiz. Current status: " + document.getProcessingStatus());
        }
        
        // Validate file path - must not be mock
        if (document.getFilePath() == null || document.getFilePath().startsWith("mock://")) {
            throw new IllegalStateException("Invalid file path: " + document.getFilePath() + ". Document must have a valid stored file.");
        }
        
        // Extract content from the document file
        String content;
        try {
            System.out.println("Extracting content from document for quiz generation...");
            content = contentExtractor.extractContent(document.getFilePath());
            
            if (content == null || content.trim().isEmpty()) {
                throw new IOException("Content extraction returned empty content");
            }
            
            System.out.println("Content extraction successful, length: " + content.length());
        } catch (Exception e) {
            System.err.println("Error extracting content from document: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to extract content from document: " + e.getMessage(), e);
        }
        
        // Create the quiz
        Quiz quiz = new Quiz();
        quiz.setTitle("Quiz: " + document.getTitle());
        quiz.setDescription("Generated quiz from " + document.getTitle());
        quiz.setTimeLimitMinutes(10);
        quiz.setDifficulty(Quiz.Difficulty.MEDIUM);
        quiz.setIsPublished(true);
        quiz.setUser(user);
        quiz.setDocument(document);
        
        // Save the quiz first to get an ID
        quiz = quizRepository.save(quiz);
        
        // Generate quiz questions using AI service
        List<QuizQuestion> questions = aiService.generateQuizQuestions(
                content, 
                document.getTitle(),
                numberOfQuestions
        );
        
        // Set quiz reference for each question and save them
        Set<QuizQuestion> savedQuestions = new HashSet<>();
        for (QuizQuestion question : questions) {
            question.setQuiz(quiz);
            question = quizQuestionRepository.save(question);
            
            // Generate answers for each question
            List<QuizAnswer> answers = aiService.generateQuizAnswers(question.getQuestionText());
            
            // Set question reference for each answer and save them
            Set<QuizAnswer> savedAnswers = new HashSet<>();
            for (int i = 0; i < answers.size(); i++) {
                QuizAnswer answer = answers.get(i);
                answer.setQuestion(question);
                answer.setOrder(i);
                answer = quizAnswerRepository.save(answer);
                savedAnswers.add(answer);
            }
            
            question.setAnswers(savedAnswers);
            savedQuestions.add(question);
        }
        
        quiz.setQuestions(savedQuestions);
        
        return quiz;
    }
    
    public Quiz generateQuizFromText(String text, String title, User user, int numberOfQuestions) {
        // Create the quiz
        Quiz quiz = new Quiz();
        quiz.setTitle("Quiz: " + title);
        quiz.setDescription("Generated quiz from text content");
        quiz.setTimeLimitMinutes(10);
        quiz.setDifficulty(Quiz.Difficulty.MEDIUM);
        quiz.setIsPublished(true);
        quiz.setUser(user);
        
        // Save the quiz first to get an ID
        quiz = quizRepository.save(quiz);
        
        // Generate quiz questions using AI service
        List<QuizQuestion> questions = aiService.generateQuizQuestions(text, title, numberOfQuestions);
        
        // Set quiz reference for each question and save them
        Set<QuizQuestion> savedQuestions = new HashSet<>();
        for (QuizQuestion question : questions) {
            question.setQuiz(quiz);
            question = quizQuestionRepository.save(question);
            
            // Generate answers for each question
            List<QuizAnswer> answers = aiService.generateQuizAnswers(question.getQuestionText());
            
            // Set question reference for each answer and save them
            Set<QuizAnswer> savedAnswers = new HashSet<>();
            for (int i = 0; i < answers.size(); i++) {
                QuizAnswer answer = answers.get(i);
                answer.setQuestion(question);
                answer.setOrder(i);
                answer = quizAnswerRepository.save(answer);
                savedAnswers.add(answer);
            }
            
            question.setAnswers(savedAnswers);
            savedQuestions.add(question);
        }
        
        quiz.setQuestions(savedQuestions);
        
        return quiz;
    }
}
