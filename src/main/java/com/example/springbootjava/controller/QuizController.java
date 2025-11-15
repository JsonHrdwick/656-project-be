package com.example.springbootjava.controller;

import com.example.springbootjava.dto.QuizResponseDTO;
import com.example.springbootjava.entity.Quiz;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.service.QuizService;
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
    private QuizService quizService;

    @GetMapping
    public ResponseEntity<List<QuizResponseDTO>> getAllQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Quiz> quizPage = quizService.getUserQuizzes(user, pageable);
        
        List<QuizResponseDTO> quizDTOs = quizPage.getContent().stream()
            .map(quiz -> {
                Double bestScore = quizService.getBestScoreForUserAndQuiz(user, quiz);
                Integer bestScoreInt = bestScore != null ? bestScore.intValue() : null;
                return new QuizResponseDTO(quiz, bestScoreInt);
            })
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/published")
    public ResponseEntity<List<QuizResponseDTO>> getPublishedQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        List<Quiz> quizzes = quizService.getPublishedQuizzes(user);
        
        List<QuizResponseDTO> quizDTOs = quizzes.stream()
            .map(quiz -> {
                Double bestScore = quizService.getBestScoreForUserAndQuiz(user, quiz);
                Integer bestScoreInt = bestScore != null ? bestScore.intValue() : null;
                return new QuizResponseDTO(quiz, bestScoreInt);
            })
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<QuizResponseDTO>> getQuizzesByDifficulty(
            @PathVariable Quiz.Difficulty difficulty,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        List<Quiz> quizzes = quizService.getQuizzesByDifficulty(user, difficulty);
        
        List<QuizResponseDTO> quizDTOs = quizzes.stream()
            .map(QuizResponseDTO::new)
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/search")
    public ResponseEntity<List<QuizResponseDTO>> searchQuizzes(
            @RequestParam String q,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        List<Quiz> quizzes = quizService.searchQuizzes(user, q);
        
        List<QuizResponseDTO> quizDTOs = quizzes.stream()
            .map(QuizResponseDTO::new)
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuizStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        long totalQuizzes = quizService.getQuizCount(user);
        long publishedQuizzes = quizService.getPublishedQuizCount(user);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuizzes", totalQuizzes);
        stats.put("publishedQuizzes", publishedQuizzes);
        stats.put("draftQuizzes", totalQuizzes - publishedQuizzes);
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping
    public ResponseEntity<QuizResponseDTO> createQuiz(@RequestBody Quiz quiz, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        quiz.setUser(user);
        quiz.setCreatedAt(java.time.LocalDateTime.now());
        quiz.setUpdatedAt(java.time.LocalDateTime.now());
        
        Quiz savedQuiz = quizService.createQuiz(quiz);
        QuizResponseDTO quizDTO = new QuizResponseDTO(savedQuiz);
        return ResponseEntity.ok(quizDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponseDTO> getQuizById(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Optional<Quiz> quizOpt = quizService.getQuizById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            if (quiz.getUser().getId().equals(user.getId())) {
                Double bestScore = quizService.getBestScoreForUserAndQuiz(user, quiz);
                Integer bestScoreInt = bestScore != null ? bestScore.intValue() : null;
                QuizResponseDTO quizDTO = new QuizResponseDTO(quiz, bestScoreInt);
                return ResponseEntity.ok(quizDTO);
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuizResponseDTO> updateQuiz(@PathVariable Long id, @RequestBody Quiz quizDetails, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Optional<Quiz> quizOpt = quizService.getQuizById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            if (quiz.getUser().getId().equals(user.getId())) {
                quiz.setTitle(quizDetails.getTitle());
                quiz.setDescription(quizDetails.getDescription());
                quiz.setTimeLimitMinutes(quizDetails.getTimeLimitMinutes());
                quiz.setDifficulty(quizDetails.getDifficulty());
                quiz.setIsPublished(quizDetails.getIsPublished());
                quiz.setUpdatedAt(java.time.LocalDateTime.now());
                
                Quiz updatedQuiz = quizService.updateQuiz(quiz);
                QuizResponseDTO quizDTO = new QuizResponseDTO(updatedQuiz);
                return ResponseEntity.ok(quizDTO);
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
        
        Optional<Quiz> quizOpt = quizService.getQuizById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            if (quiz.getUser().getId().equals(user.getId())) {
                quizService.deleteQuiz(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitQuiz(
            @PathVariable Long id,
            @RequestBody Map<String, Object> submissionData,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Double score = ((Number) submissionData.get("score")).doubleValue();
            Integer timeSpentMinutes = submissionData.get("timeSpentMinutes") != null 
                ? ((Number) submissionData.get("timeSpentMinutes")).intValue() 
                : null;
            
            quizService.submitQuizAttempt(id, user, score, timeSpentMinutes);
            
            // Get updated quiz with best score
            Optional<Quiz> quizOpt = quizService.getQuizById(id);
            if (quizOpt.isPresent()) {
                Quiz quiz = quizOpt.get();
                Double bestScore = quizService.getBestScoreForUserAndQuiz(user, quiz);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("score", score);
                response.put("bestScore", bestScore);
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}