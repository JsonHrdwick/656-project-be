package com.example.springbootjava.service;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.FlashcardRepository;
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
        if (document.getContent() == null || document.getContent().isEmpty()) {
            throw new RuntimeException("Document content is empty or not processed yet");
        }
        
        List<Flashcard> flashcards = aiService.generateFlashcards(
                document.getContent(), 
                document.getTitle()
        );
        
        // Set user and document for each flashcard
        for (Flashcard flashcard : flashcards) {
            flashcard.setUser(user);
            flashcard.setDocument(document);
        }
        
        return flashcardRepository.saveAll(flashcards);
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
