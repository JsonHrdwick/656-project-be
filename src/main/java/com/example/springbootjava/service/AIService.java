package com.example.springbootjava.service;

import com.example.springbootjava.config.OpenAIConfig;
import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.QuizQuestion;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIService {
    
    @Autowired
    private OpenAiService openAiService;
    
    @Autowired
    private OpenAIConfig openAIConfig;
    
    public String generateSummary(String content) {
        try {
            // Limit content to avoid token limits
            String limitedContent = content.length() > 3000 ? content.substring(0, 3000) : content;
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an AI assistant that creates concise, informative summaries. " +
                "Summarize the following content in 2-3 sentences, highlighting the key points:"));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), limitedContent));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(openAIConfig.getModel())
                .messages(messages)
                .maxTokens(200)
                .temperature(0.7)
                .build();
            
            return openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
                
        } catch (Exception e) {
            // Fallback to simple summary if API fails
            return content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }
    }
    
    public List<String> extractKeyConcepts(String content) {
        try {
            // Limit content to avoid token limits
            String limitedContent = content.length() > 2000 ? content.substring(0, 2000) : content;
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an AI assistant that extracts key concepts from text. " +
                "Extract 5-7 important concepts, terms, or topics from the following content. " +
                "Return them as a comma-separated list, one concept per line:"));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), limitedContent));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(openAIConfig.getModel())
                .messages(messages)
                .maxTokens(150)
                .temperature(0.3)
                .build();
            
            String response = openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
            
            // Parse the response into a list
            List<String> concepts = new ArrayList<>();
            String[] lines = response.split("\n");
            for (String line : lines) {
                String concept = line.trim().replaceAll("^[0-9]+\\.\\s*", ""); // Remove numbering
                if (!concept.isEmpty() && concept.length() > 2) {
                    concepts.add(concept);
                }
                if (concepts.size() >= 7) break;
            }
            return concepts;
            
        } catch (Exception e) {
            // Fallback to simple word extraction
            List<String> concepts = new ArrayList<>();
            String limitedContent = content.length() > 1000 ? content.substring(0, 1000) : content;
            String[] words = limitedContent.split("\\s+");
            for (String word : words) {
                if (word.length() > 5 && word.matches("[a-zA-Z]+")) {
                    concepts.add(word);
                    if (concepts.size() >= 5) break;
                }
            }
            return concepts;
        }
    }
    
    public List<Flashcard> generateFlashcards(String content, String category) {
        try {
            // Limit content to avoid token limits
            String limitedContent = content.length() > 3000 ? content.substring(0, 3000) : content;
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an AI assistant that creates educational flashcards. " +
                "Create 3-5 flashcards from the following content. " +
                "Format each flashcard as: QUESTION|ANSWER|DIFFICULTY " +
                "Where DIFFICULTY is EASY, MEDIUM, or HARD. " +
                "Each line should be one flashcard:"));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), 
                "Content: " + limitedContent + "\nCategory: " + category));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(openAIConfig.getModel())
                .messages(messages)
                .maxTokens(400)
                .temperature(0.7)
                .build();
            
            String response = openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
            
            // Parse the response into flashcards
            List<Flashcard> flashcards = new ArrayList<>();
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        Flashcard flashcard = new Flashcard();
                        flashcard.setQuestion(parts[0].trim());
                        flashcard.setAnswer(parts[1].trim());
                        flashcard.setCategory(category);
                        
                        // Set difficulty
                        if (parts.length >= 3) {
                            try {
                                flashcard.setDifficulty(Flashcard.Difficulty.valueOf(parts[2].trim().toUpperCase()));
                            } catch (IllegalArgumentException e) {
                                flashcard.setDifficulty(Flashcard.Difficulty.MEDIUM);
                            }
                        } else {
                            flashcard.setDifficulty(Flashcard.Difficulty.MEDIUM);
                        }
                        
                        flashcards.add(flashcard);
                    }
                }
            }
            
            return flashcards;
            
        } catch (Exception e) {
            // Fallback to simple flashcard generation
            List<Flashcard> flashcards = new ArrayList<>();
            String limitedContent = content.length() > 2000 ? content.substring(0, 2000) : content;
            String[] sentences = limitedContent.split("[.!?]+");
            for (int i = 0; i < Math.min(2, sentences.length); i++) {
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
    }
    
    public List<QuizQuestion> generateQuizQuestions(String content, int numberOfQuestions) {
        try {
            // Limit content and questions to avoid token limits
            String limitedContent = content.length() > 3000 ? content.substring(0, 3000) : content;
            int maxQuestions = Math.min(numberOfQuestions, 5); // Cap at 5 questions
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an AI assistant that creates quiz questions. " +
                "Create " + maxQuestions + " multiple choice questions from the following content. " +
                "Format each question as: QUESTION|OPTION_A|OPTION_B|OPTION_C|OPTION_D|CORRECT_ANSWER|EXPLANATION " +
                "Where CORRECT_ANSWER is A, B, C, or D. Each line should be one question:"));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), limitedContent));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(openAIConfig.getModel())
                .messages(messages)
                .maxTokens(600)
                .temperature(0.7)
                .build();
            
            String response = openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
            
            // Parse the response into quiz questions
            List<QuizQuestion> questions = new ArrayList<>();
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6) {
                        QuizQuestion question = new QuizQuestion();
                        question.setQuestionText(parts[0].trim());
                        
                        // Create options (simplified - in a real app you'd have separate option fields)
                        String options = "A) " + parts[1].trim() + "\n" +
                                       "B) " + parts[2].trim() + "\n" +
                                       "C) " + parts[3].trim() + "\n" +
                                       "D) " + parts[4].trim();
                        question.setQuestionText(question.getQuestionText() + "\n\n" + options);
                        // Align with updated QuizQuestion model (answers stored in QuizAnswer)
                        question.setQuestionType(QuizQuestion.QuestionType.MULTIPLE_CHOICE);
                        question.setPoints(1);
                        questions.add(question);
                    }
                }
            }
            
            return questions;
            
        } catch (Exception e) {
            // Fallback to simple question generation
            List<QuizQuestion> questions = new ArrayList<>();
            String limitedContent = content.length() > 2000 ? content.substring(0, 2000) : content;
            int maxQuestions = Math.min(numberOfQuestions, 3);
            String[] sentences = limitedContent.split("[.!?]+");
            for (int i = 0; i < Math.min(maxQuestions, sentences.length); i++) {
                if (sentences[i].trim().length() > 10) {
                    QuizQuestion question = new QuizQuestion();
                    question.setQuestionText("What is the main topic of: " + sentences[i].trim() + "?");
                    question.setQuestionType(QuizQuestion.QuestionType.MULTIPLE_CHOICE);
                    question.setPoints(1);
                    questions.add(question);
                }
            }
            return questions;
        }
    }
    
    public String answerQuestion(String question, String context) {
        try {
            // Limit context to avoid token limits
            String limitedContext = context.length() > 2000 ? context.substring(0, 2000) : context;
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an AI assistant that answers questions based on provided context. " +
                "Answer the question using only the information from the context. " +
                "If the context doesn't contain enough information, say so. " +
                "Keep your answer concise and accurate."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), 
                "Context: " + limitedContext + "\n\nQuestion: " + question));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(openAIConfig.getModel())
                .messages(messages)
                .maxTokens(300)
                .temperature(0.3)
                .build();
            
            return openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
                
        } catch (Exception e) {
            // Fallback to simple response
            return "I'm unable to answer that question at the moment. Please try again later.";
        }
    }
}
