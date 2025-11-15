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

import java.io.IOException;
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
        // Validate document is processed
        if (document.getProcessingStatus() != Document.ProcessingStatus.COMPLETED) {
            throw new IllegalStateException("Document must be processed before generating flashcards. Current status: " + document.getProcessingStatus());
        }
        
        // Validate file path - must not be mock
        if (document.getFilePath() == null || document.getFilePath().startsWith("mock://")) {
            throw new IllegalStateException("Invalid file path: " + document.getFilePath() + ". Document must have a valid stored file.");
        }
        
        // Extract content from the document file
        String content;
        try {
            System.out.println("=== FLASHCARD GENERATION START ===");
            System.out.println("Document ID: " + document.getId());
            System.out.println("Document title: " + document.getTitle());
            System.out.println("Document file path: " + document.getFilePath());
            System.out.println("Document file type: " + document.getFileType());
            
            System.out.println("Extracting real content from file...");
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
        
        System.out.println("Generating flashcards using AI service...");
        List<Flashcard> flashcards = aiService.generateFlashcards(
                content, 
                document.getTitle()
        );
        System.out.println("AI service generated " + flashcards.size() + " flashcards");
        
        // Set user and document for each flashcard
        for (Flashcard flashcard : flashcards) {
            flashcard.setUser(user);
            flashcard.setDocument(document);
        }
        
        List<Flashcard> savedFlashcards = flashcardRepository.saveAll(flashcards);
        System.out.println("Saved " + savedFlashcards.size() + " flashcards to database");
        System.out.println("=== FLASHCARD GENERATION END ===");
        
        return savedFlashcards;
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
