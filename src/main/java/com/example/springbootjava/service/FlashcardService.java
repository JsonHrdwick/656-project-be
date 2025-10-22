package com.example.springbootjava.service;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.FlashcardRepository;
import com.example.springbootjava.service.DocumentContentExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FlashcardService {
    
    @Autowired
    private FlashcardRepository flashcardRepository;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private DocumentContentExtractor contentExtractor;
    
    public List<Flashcard> getUserFlashcards(User user) {
        return flashcardRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Page<Flashcard> getUserFlashcards(User user, Pageable pageable) {
        return flashcardRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    public List<Flashcard> getFlashcardsByDifficulty(User user, Flashcard.Difficulty difficulty) {
        return flashcardRepository.findByUserAndDifficulty(user, difficulty);
    }
    
    public List<Flashcard> getFlashcardsByCategory(User user, String category) {
        return flashcardRepository.findByUserAndCategory(user, category);
    }
    
    public List<Flashcard> searchFlashcards(User user, String searchTerm) {
        return flashcardRepository.findByUserAndSearchTerm(user, searchTerm);
    }
    
    public List<Flashcard> getRandomFlashcards(User user, int limit) {
        return flashcardRepository.findRandomByUser(user, limit);
    }
    
    public long getFlashcardCount(User user) {
        return flashcardRepository.countByUser(user);
    }
    
    public long getFlashcardCountByDifficulty(User user, Flashcard.Difficulty difficulty) {
        return flashcardRepository.countByUserAndDifficulty(user, difficulty);
    }
    
    public Flashcard createFlashcard(Flashcard flashcard) {
        return flashcardRepository.save(flashcard);
    }
    
    public Optional<Flashcard> getFlashcardById(Long id) {
        return flashcardRepository.findById(id);
    }
    
    public Flashcard updateFlashcard(Flashcard flashcard) {
        return flashcardRepository.save(flashcard);
    }
    
    public void deleteFlashcard(Long id) {
        flashcardRepository.deleteById(id);
    }
    
    public List<Flashcard> generateFlashcardsFromDocument(Document document, User user) {
        // Extract content from the document file since content is not stored in database
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
        
        List<Flashcard> flashcards = aiService.generateFlashcards(
                content, 
                document.getTitle()
        );
        
        // Set user and document for each flashcard
        for (Flashcard flashcard : flashcards) {
            flashcard.setUser(user);
            flashcard.setDocument(document);
        }
        
        return flashcardRepository.saveAll(flashcards);
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
    
    public List<Flashcard> generateFlashcardsFromText(String text, String category, User user) {
        List<Flashcard> flashcards = aiService.generateFlashcards(text, category);
        
        // Set user for each flashcard
        for (Flashcard flashcard : flashcards) {
            flashcard.setUser(user);
        }
        
        return flashcardRepository.saveAll(flashcards);
    }
}
