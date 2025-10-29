package com.example.springbootjava.dto;

import com.example.springbootjava.entity.QuizQuestion;
import com.example.springbootjava.entity.QuizAnswer;

import java.util.List;
import java.util.stream.Collectors;

public class QuizQuestionResponseDTO {
    private Long id;
    private String question;
    private List<String> options;
    private Integer correctAnswer;
    private String explanation;

    public QuizQuestionResponseDTO() {}

    public QuizQuestionResponseDTO(QuizQuestion quizQuestion) {
        this.id = quizQuestion.getId();
        this.question = quizQuestion.getQuestionText();
        
        // Convert QuizAnswer entities to options array and find correct answer
        if (quizQuestion.getAnswers() != null) {
            List<QuizAnswer> sortedAnswers = quizQuestion.getAnswers().stream()
                .sorted((a, b) -> {
                    if (a.getOrder() == null && b.getOrder() == null) return 0;
                    if (a.getOrder() == null) return 1;
                    if (b.getOrder() == null) return -1;
                    return a.getOrder().compareTo(b.getOrder());
                })
                .collect(Collectors.toList());
            
            this.options = sortedAnswers.stream()
                .map(QuizAnswer::getAnswerText)
                .collect(Collectors.toList());
            
            // Find the index of the correct answer
            for (int i = 0; i < sortedAnswers.size(); i++) {
                if (Boolean.TRUE.equals(sortedAnswers.get(i).getIsCorrect())) {
                    this.correctAnswer = i;
                    break;
                }
            }
            
            // If no correct answer found, default to 0
            if (this.correctAnswer == null) {
                this.correctAnswer = 0;
            }
        } else {
            this.options = List.of("Option A", "Option B", "Option C", "Option D");
            this.correctAnswer = 0;
        }
        
        this.explanation = "This is a sample explanation. In production, AI would generate a proper explanation.";
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

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Integer getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(Integer correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
