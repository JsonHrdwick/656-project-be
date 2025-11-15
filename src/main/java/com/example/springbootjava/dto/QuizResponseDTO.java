package com.example.springbootjava.dto;

import com.example.springbootjava.entity.Quiz;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class QuizResponseDTO {
    private Long id;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private String difficulty;
    private Boolean isPublished;
    private String createdAt;
    private String updatedAt;
    private List<QuizQuestionResponseDTO> questions;
    private Boolean completed;
    private Integer score;
    private Long documentId;
    private String documentTitle;

    public QuizResponseDTO() {}

    public QuizResponseDTO(Quiz quiz) {
        this(quiz, null);
    }
    
    public QuizResponseDTO(Quiz quiz, Integer bestScore) {
        this.id = quiz.getId();
        this.title = quiz.getTitle();
        this.description = quiz.getDescription();
        this.timeLimitMinutes = quiz.getTimeLimitMinutes();
        this.difficulty = quiz.getDifficulty() != null ? quiz.getDifficulty().toString() : "MEDIUM";
        this.isPublished = quiz.getIsPublished();
        this.createdAt = quiz.getCreatedAt() != null ? quiz.getCreatedAt().toString() : LocalDateTime.now().toString();
        this.updatedAt = quiz.getUpdatedAt() != null ? quiz.getUpdatedAt().toString() : LocalDateTime.now().toString();
        
        // Convert QuizQuestion entities to DTOs
        if (quiz.getQuestions() != null) {
            this.questions = quiz.getQuestions().stream()
                .map(QuizQuestionResponseDTO::new)
                .collect(Collectors.toList());
        }
        
        this.completed = false; // Default value
        this.score = bestScore; // Set best score if provided
        
        // Include document information if available
        if (quiz.getDocument() != null) {
            this.documentId = quiz.getDocument().getId();
            this.documentTitle = quiz.getDocument().getTitle();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<QuizQuestionResponseDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestionResponseDTO> questions) {
        this.questions = questions;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }
}
