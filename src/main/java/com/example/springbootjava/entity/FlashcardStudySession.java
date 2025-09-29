package com.example.springbootjava.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "flashcard_study_sessions")
public class FlashcardStudySession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "is_correct")
    private Boolean isCorrect;
    
    @Column(name = "response_time_seconds")
    private Integer responseTimeSeconds;
    
    @Column(name = "studied_at")
    private LocalDateTime studiedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    public FlashcardStudySession() {
        this.studiedAt = LocalDateTime.now();
    }
    
    public FlashcardStudySession(Boolean isCorrect, Flashcard flashcard, User user) {
        this();
        this.isCorrect = isCorrect;
        this.flashcard = flashcard;
        this.user = user;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Boolean getIsCorrect() {
        return isCorrect;
    }
    
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    
    public Integer getResponseTimeSeconds() {
        return responseTimeSeconds;
    }
    
    public void setResponseTimeSeconds(Integer responseTimeSeconds) {
        this.responseTimeSeconds = responseTimeSeconds;
    }
    
    public LocalDateTime getStudiedAt() {
        return studiedAt;
    }
    
    public void setStudiedAt(LocalDateTime studiedAt) {
        this.studiedAt = studiedAt;
    }
    
    public Flashcard getFlashcard() {
        return flashcard;
    }
    
    public void setFlashcard(Flashcard flashcard) {
        this.flashcard = flashcard;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
}
