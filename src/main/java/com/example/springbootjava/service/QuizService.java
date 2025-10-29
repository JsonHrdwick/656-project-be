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
        // Extract content from the document file
        String content;
        try {
            if (document.getFilePath() != null && !document.getFilePath().startsWith("mock://")) {
                // Extract real content from file
                content = contentExtractor.extractContent(document.getFilePath());
            } else {
                // Generate simulated content for mock files or when file path is not available
                content = generateSimulatedContent(document.getFileType(), document.getTitle());
            }
        } catch (Exception e) {
            System.err.println("Error extracting content from document: " + e.getMessage());
            // Fallback to simulated content
            content = generateSimulatedContent(document.getFileType(), document.getTitle());
        }
        
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Could not extract content from document");
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
    
    private String generateSimulatedContent(String fileType, String title) {
        // Generate realistic sample content based on file type for POC demonstration
        StringBuilder content = new StringBuilder();
        
        content.append("Document Title: ").append(title).append("\n\n");
        content.append("File Type: ").append(fileType).append("\n\n");
        
        switch (fileType.toLowerCase()) {
            case "pdf":
                content.append("This is a simulated PDF document content for demonstration purposes.\n\n");
                content.append("Chapter 1: Introduction\n");
                content.append("This document contains important information about the topic. ");
                content.append("The content would normally be extracted from the actual PDF file using Apache Tika or similar libraries.\n\n");
                content.append("Chapter 2: Main Concepts\n");
                content.append("Key concepts include data structures, algorithms, and system design. ");
                content.append("These topics are essential for understanding modern software development.\n\n");
                content.append("Chapter 3: Implementation\n");
                content.append("When implementing solutions, consider performance, scalability, and maintainability. ");
                content.append("Always follow best practices and coding standards.\n\n");
                content.append("Chapter 4: Testing\n");
                content.append("Comprehensive testing ensures code quality and reliability. ");
                content.append("Include unit tests, integration tests, and end-to-end tests.\n\n");
                content.append("Chapter 5: Conclusion\n");
                content.append("This document provides a foundation for understanding the subject matter. ");
                content.append("Continue learning and practicing to master these concepts.\n");
                break;
            case "docx":
            case "doc":
                content.append("This is a simulated Word document content for demonstration purposes.\n\n");
                content.append("Section 1: Overview\n");
                content.append("This document outlines the key principles and methodologies.\n\n");
                content.append("Section 2: Detailed Analysis\n");
                content.append("A comprehensive analysis of the subject matter reveals important insights.\n\n");
                content.append("Section 3: Recommendations\n");
                content.append("Based on the analysis, several recommendations can be made.\n");
                break;
            case "txt":
                content.append("This is a simulated text document content for demonstration purposes.\n\n");
                content.append("This document contains plain text content that would be used for generating flashcards and quizzes.\n");
                content.append("The content includes various topics and concepts that are important for learning.\n");
                break;
            default:
                content.append("This is a simulated document content for demonstration purposes.\n\n");
                content.append("The document contains various topics and concepts that are important for learning.\n");
                break;
        }
        
        return content.toString();
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
