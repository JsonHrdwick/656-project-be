package com.example.springbootjava.controller;

import com.example.springbootjava.entity.Quiz;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.QuizRepository;
import com.example.springbootjava.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @GetMapping
    public ResponseEntity<List<Quiz>> getAllQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Quiz> quizPage = quizRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        return ResponseEntity.ok(quizPage.getContent());
    }

    @GetMapping("/published")
    public ResponseEntity<List<Quiz>> getPublishedQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        List<Quiz> quizzes = quizRepository.findPublishedByUser(user);
        
        return ResponseEntity.ok(quizzes);
    }

    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<Quiz>> getQuizzesByDifficulty(
            @PathVariable Quiz.Difficulty difficulty,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        List<Quiz> quizzes = quizRepository.findByUserAndDifficulty(user, difficulty);
        
        return ResponseEntity.ok(quizzes);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Quiz>> searchQuizzes(
            @RequestParam String q,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        List<Quiz> quizzes = quizRepository.findByUserAndSearchTerm(user, q);
        
        return ResponseEntity.ok(quizzes);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuizStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        long totalQuizzes = quizRepository.countByUser(user);
        long publishedQuizzes = quizRepository.countPublishedByUser(user);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuizzes", totalQuizzes);
        stats.put("publishedQuizzes", publishedQuizzes);
        stats.put("draftQuizzes", totalQuizzes - publishedQuizzes);
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping
    public ResponseEntity<Quiz> createQuiz(@RequestBody Quiz quiz, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        quiz.setUser(user);
        quiz.setCreatedAt(java.time.LocalDateTime.now());
        quiz.setUpdatedAt(java.time.LocalDateTime.now());
        
        Quiz savedQuiz = quizRepository.save(quiz);
        return ResponseEntity.ok(savedQuiz);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Optional<Quiz> quizOpt = quizRepository.findById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            if (quiz.getUser().getId().equals(user.getId())) {
                return ResponseEntity.ok(quiz);
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Quiz> updateQuiz(@PathVariable Long id, @RequestBody Quiz quizDetails, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Optional<Quiz> quizOpt = quizRepository.findById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            if (quiz.getUser().getId().equals(user.getId())) {
                quiz.setTitle(quizDetails.getTitle());
                quiz.setDescription(quizDetails.getDescription());
                quiz.setTimeLimitMinutes(quizDetails.getTimeLimitMinutes());
                quiz.setDifficulty(quizDetails.getDifficulty());
                quiz.setIsPublished(quizDetails.getIsPublished());
                quiz.setUpdatedAt(java.time.LocalDateTime.now());
                
                Quiz updatedQuiz = quizRepository.save(quiz);
                return ResponseEntity.ok(updatedQuiz);
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Optional<Quiz> quizOpt = quizRepository.findById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            if (quiz.getUser().getId().equals(user.getId())) {
                quizRepository.delete(quiz);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}