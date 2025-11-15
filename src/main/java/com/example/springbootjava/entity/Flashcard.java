package com.example.springbootjava.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Entity
@Table(name = "flashcards")
public class Flashcard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "question", columnDefinition = "TEXT")
    private String question;
    
    @NotBlank
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;
    
    @Column(name = "category")
    private String category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private Difficulty difficulty = Difficulty.MEDIUM;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @JsonBackReference
    private Document document;
    
    @OneToMany(mappedBy = "flashcard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.Set<FlashcardStudySession> studySessions;
    
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
    
    public Flashcard() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Flashcard(String question, String answer, User user) {
        this();
        this.question = question;
        this.answer = answer;
        this.user = user;
    }
    
    public Flashcard(String question, String answer, String category, Difficulty difficulty, User user, Document document) {
        this(question, answer, user);
        this.category = category;
        this.difficulty = difficulty;
        this.document = document;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Document getDocument() {
        return document;
    }
    
    public void setDocument(Document document) {
        this.document = document;
    }
    
    public java.util.Set<FlashcardStudySession> getStudySessions() {
        return studySessions;
    }
    
    public void setStudySessions(java.util.Set<FlashcardStudySession> studySessions) {
        this.studySessions = studySessions;
    }

    @JsonProperty("documentId")
    public Long getDocumentId() {
        return document != null ? document.getId() : null;
    }

    @JsonProperty("document")
    public DocumentInfo getDocumentInfo() {
        if (document == null) {
            return null;
        }
        return new DocumentInfo(document.getId(), document.getTitle());
    }

    // Inner class for document info
    public static class DocumentInfo {
        private Long id;
        private String title;

        public DocumentInfo(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }
}
