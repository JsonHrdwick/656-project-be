package com.example.springbootjava.controller;

import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.service.FlashcardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {
    
    @Autowired
    private FlashcardService flashcardService;
    
    @GetMapping
    public ResponseEntity<List<Flashcard>> getUserFlashcards(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Flashcard> flashcards = flashcardService.getUserFlashcards(user);
        return ResponseEntity.ok(flashcards);
    }
    
    @GetMapping("/page")
    public ResponseEntity<Page<Flashcard>> getUserFlashcardsPage(Pageable pageable,
                                                               Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<Flashcard> flashcards = flashcardService.getUserFlashcards(user, pageable);
        return ResponseEntity.ok(flashcards);
    }
    
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<Flashcard>> getFlashcardsByDifficulty(@PathVariable String difficulty,
                                                                    Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        try {
            Flashcard.Difficulty diff = Flashcard.Difficulty.valueOf(difficulty.toUpperCase());
            List<Flashcard> flashcards = flashcardService.getFlashcardsByDifficulty(user, diff);
            return ResponseEntity.ok(flashcards);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Flashcard>> getFlashcardsByCategory(@PathVariable String category,
                                                                  Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Flashcard> flashcards = flashcardService.getFlashcardsByCategory(user, category);
        return ResponseEntity.ok(flashcards);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Flashcard>> searchFlashcards(@RequestParam String q,
                                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Flashcard> flashcards = flashcardService.searchFlashcards(user, q);
        return ResponseEntity.ok(flashcards);
    }
    
    @GetMapping("/random")
    public ResponseEntity<List<Flashcard>> getRandomFlashcards(@RequestParam(defaultValue = "10") int limit,
                                                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Flashcard> flashcards = flashcardService.getRandomFlashcards(user, limit);
        return ResponseEntity.ok(flashcards);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFlashcardStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        long totalFlashcards = flashcardService.getFlashcardCount(user);
        long easyFlashcards = flashcardService.getFlashcardCountByDifficulty(user, Flashcard.Difficulty.EASY);
        long mediumFlashcards = flashcardService.getFlashcardCountByDifficulty(user, Flashcard.Difficulty.MEDIUM);
        long hardFlashcards = flashcardService.getFlashcardCountByDifficulty(user, Flashcard.Difficulty.HARD);
        
        return ResponseEntity.ok(Map.of(
                "totalFlashcards", totalFlashcards,
                "easyFlashcards", easyFlashcards,
                "mediumFlashcards", mediumFlashcards,
                "hardFlashcards", hardFlashcards
        ));
    }
    
    @PostMapping
    public ResponseEntity<Flashcard> createFlashcard(@RequestBody Flashcard flashcard,
                                                   Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        flashcard.setUser(user);
        Flashcard createdFlashcard = flashcardService.createFlashcard(flashcard);
        return ResponseEntity.ok(createdFlashcard);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Flashcard> getFlashcard(@PathVariable Long id,
                                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Flashcard> flashcard = flashcardService.getFlashcardById(id);
        
        if (flashcard.isPresent() && flashcard.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.ok(flashcard.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Flashcard> updateFlashcard(@PathVariable Long id,
                                                   @RequestBody Flashcard flashcard,
                                                   Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Flashcard> existingFlashcard = flashcardService.getFlashcardById(id);
        
        if (existingFlashcard.isPresent() && existingFlashcard.get().getUser().getId().equals(user.getId())) {
            flashcard.setId(id);
            flashcard.setUser(user);
            Flashcard updatedFlashcard = flashcardService.updateFlashcard(flashcard);
            return ResponseEntity.ok(updatedFlashcard);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFlashcard(@PathVariable Long id,
                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Flashcard> flashcard = flashcardService.getFlashcardById(id);
        
        if (flashcard.isPresent() && flashcard.get().getUser().getId().equals(user.getId())) {
            flashcardService.deleteFlashcard(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
