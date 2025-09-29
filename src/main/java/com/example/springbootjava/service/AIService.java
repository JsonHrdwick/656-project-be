package com.example.springbootjava.service;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.Quiz;
import com.example.springbootjava.entity.QuizQuestion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIService {
    
    public String generateSummary(String content) {
        // Simplified implementation - in production, integrate with OpenAI
        if (content.length() > 500) {
            return content.substring(0, 500) + "... [AI Summary would be generated here]";
        }
        return content + " [AI Summary would be generated here]";
    }
    
    public List<String> extractKeyConcepts(String content) {
        // Simplified implementation - in production, use AI to extract concepts
        List<String> concepts = new ArrayList<>();
        String[] words = content.split("\\s+");
        for (String word : words) {
            if (word.length() > 5 && word.matches("[a-zA-Z]+")) {
                concepts.add(word);
                if (concepts.size() >= 10) break;
            }
        }
        return concepts;
    }
    
    public List<Flashcard> generateFlashcards(String content, String category) {
        // Simplified implementation - in production, use AI to generate flashcards
        List<Flashcard> flashcards = new ArrayList<>();
        
        // Create sample flashcards based on content
        String[] sentences = content.split("[.!?]+");
        for (int i = 0; i < Math.min(3, sentences.length); i++) {
            if (sentences[i].trim().length() > 10) {
                Flashcard flashcard = new Flashcard();
                flashcard.setQuestion("What is the main point of: " + sentences[i].trim() + "?");
                flashcard.setAnswer("This is a sample answer. In production, AI would generate a proper answer.");
                flashcard.setCategory(category);
                flashcard.setDifficulty(Flashcard.Difficulty.MEDIUM);
                flashcards.add(flashcard);
            }
        }
        
        return flashcards;
    }
    
    public List<QuizQuestion> generateQuizQuestions(String content, int numberOfQuestions) {
        // Simplified implementation - in production, use AI to generate quiz questions
        List<QuizQuestion> questions = new ArrayList<>();
        
        // Create sample questions based on content
        String[] sentences = content.split("[.!?]+");
        for (int i = 0; i < Math.min(numberOfQuestions, sentences.length); i++) {
            if (sentences[i].trim().length() > 10) {
                QuizQuestion question = new QuizQuestion();
                question.setQuestionText("What is the main topic of: " + sentences[i].trim() + "?");
                question.setCorrectAnswer("Sample correct answer");
                question.setExplanation("This is a sample explanation. In production, AI would generate proper content.");
                questions.add(question);
            }
        }
        
        return questions;
    }
    
    public String answerQuestion(String question, String context) {
        // Simplified implementation - in production, use AI to answer questions
        return "This is a sample answer based on the context. In production, AI would provide a proper answer to: " + question;
    }
}
