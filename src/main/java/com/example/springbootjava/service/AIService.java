package com.example.springbootjava.service;

import com.example.springbootjava.config.OpenAIConfig;
import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.QuizAnswer;
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
            System.err.println("AI Service failed to generate flashcards: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to simple flashcard generation based on actual content
            List<Flashcard> flashcards = new ArrayList<>();
            String limitedContent = content.length() > 2000 ? content.substring(0, 2000) : content;
            
            // Try to extract meaningful sentences from the content
            String[] sentences = limitedContent.split("[.!?]+");
            int flashcardCount = 0;
            
            for (String sentence : sentences) {
                if (sentence.trim().length() > 20 && flashcardCount < 3) {
                    Flashcard flashcard = new Flashcard();
                    flashcard.setQuestion("What does this statement mean: \"" + sentence.trim() + "\"?");
                    flashcard.setAnswer("This statement discusses: " + sentence.trim() + ". (AI service temporarily unavailable - using content-based fallback)");
                    flashcard.setCategory(category);
                    flashcard.setDifficulty(Flashcard.Difficulty.MEDIUM);
                    flashcards.add(flashcard);
                    flashcardCount++;
                }
            }
            
            // If no meaningful sentences found, create a generic flashcard
            if (flashcards.isEmpty()) {
                Flashcard flashcard = new Flashcard();
                flashcard.setQuestion("What is the main topic of this " + category + " document?");
                flashcard.setAnswer("This document covers topics related to " + category + ". (AI service temporarily unavailable)");
                flashcard.setCategory(category);
                flashcard.setDifficulty(Flashcard.Difficulty.MEDIUM);
                flashcards.add(flashcard);
            }
            
            System.out.println("Generated " + flashcards.size() + " fallback flashcards due to AI service error");
            return flashcards;
        }
    }
    
    public List<QuizQuestion> generateQuizQuestions(String content, String title, int numberOfQuestions) {
        try {
            // Limit content and questions to avoid token limits
            String limitedContent = content.length() > 3000 ? content.substring(0, 3000) : content;
            int maxQuestions = Math.min(numberOfQuestions, 5); // Cap at 5 questions
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an AI assistant that creates quiz questions. " +
                "Create " + maxQuestions + " multiple choice questions from the following content. " +
                "IMPORTANT: Format each question EXACTLY as follows (one question per line):\n" +
                "QUESTION_TEXT|OPTION_A|OPTION_B|OPTION_C|OPTION_D|CORRECT_LETTER\n" +
                "Where:\n" +
                "- QUESTION_TEXT is just the question text (no options, no letters)\n" +
                "- OPTION_A, OPTION_B, OPTION_C, OPTION_D are the four answer options (just the text, no letters)\n" +
                "- CORRECT_LETTER is exactly A, B, C, or D (the letter of the correct answer)\n" +
                "Do NOT include the question text with options embedded. Keep them separate."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), limitedContent));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(openAIConfig.getModel())
                .messages(messages)
                .maxTokens(1000)
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
                line = line.trim();
                if (line.isEmpty() || !line.contains("|")) continue;
                
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    QuizQuestion question = new QuizQuestion();
                    // Question text should NOT include options
                    question.setQuestionText(parts[0].trim());
                    question.setQuestionType(QuizQuestion.QuestionType.MULTIPLE_CHOICE);
                    question.setPoints(1);
                    questions.add(question);
                }
            }
            
            return questions;
            
        } catch (Exception e) {
            System.err.println("Error generating quiz questions: " + e.getMessage());
            e.printStackTrace();
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
    
    /**
     * Generates complete quiz questions with answers in one call.
     * Returns a list where each element contains both question and answer data.
     */
    public static class QuestionWithAnswers {
        public String questionText;
        public String optionA;
        public String optionB;
        public String optionC;
        public String optionD;
        public String correctAnswer; // A, B, C, or D
    }
    
    public List<QuestionWithAnswers> generateQuizQuestionsWithAnswers(String content, String title, int numberOfQuestions) {
        try {
            // Limit content and questions to avoid token limits
            String limitedContent = content.length() > 3000 ? content.substring(0, 3000) : content;
            int maxQuestions = Math.min(numberOfQuestions, 5); // Cap at 5 questions
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an AI assistant that creates quiz questions with multiple choice answers. " +
                "Create " + maxQuestions + " multiple choice questions from the following content. " +
                "IMPORTANT: Format each question EXACTLY as follows (one question per line, no blank lines between questions):\n" +
                "QUESTION_TEXT|OPTION_A|OPTION_B|OPTION_C|OPTION_D|CORRECT_LETTER\n" +
                "Where:\n" +
                "- QUESTION_TEXT is just the question text (no options, no letters like A) B) C) D))\n" +
                "- OPTION_A, OPTION_B, OPTION_C, OPTION_D are the four answer options (just the text, no letters)\n" +
                "- CORRECT_LETTER is exactly A, B, C, or D (the letter of the correct answer)\n" +
                "Example format:\n" +
                "What is the capital of France?|Paris|London|Berlin|Madrid|A\n" +
                "Do NOT include letters (A) B) C) D)) in the question text or options. Keep them separate."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), "Content: " + limitedContent));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(openAIConfig.getModel())
                .messages(messages)
                .maxTokens(1200)
                .temperature(0.7)
                .build();
            
            String response = openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
            
            System.out.println("=== QUIZ GENERATION RESPONSE ===");
            System.out.println(response);
            System.out.println("=== END RESPONSE ===");
            
            // Parse the response into questions with answers
            List<QuestionWithAnswers> questions = new ArrayList<>();
            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Skip lines that are just labels or headers
                if (line.toLowerCase().startsWith("question") || 
                    line.toLowerCase().startsWith("answer") ||
                    line.matches("^[A-D]\\)\\s*")) {
                    continue;
                }
                
                // Try to parse pipe-separated format
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6) {
                        QuestionWithAnswers qwa = new QuestionWithAnswers();
                        qwa.questionText = parts[0].trim();
                        qwa.optionA = parts[1].trim();
                        qwa.optionB = parts[2].trim();
                        qwa.optionC = parts[3].trim();
                        qwa.optionD = parts[4].trim();
                        qwa.correctAnswer = parts[5].trim().toUpperCase();
                        
                        // Validate correct answer is A, B, C, or D
                        if (!qwa.correctAnswer.matches("[ABCD]")) {
                            System.err.println("Invalid correct answer: " + qwa.correctAnswer + ", defaulting to A");
                            qwa.correctAnswer = "A";
                        }
                        
                        // Clean up question text - remove any embedded options
                        qwa.questionText = qwa.questionText.replaceAll("(?i)\\s*[A-D]\\)\\s*", " ").trim();
                        qwa.questionText = qwa.questionText.replaceAll("QUESTION\\s*:?\\s*", "").trim();
                        
                        // Clean up options - remove any leading letters
                        qwa.optionA = qwa.optionA.replaceAll("^[A-D]\\)\\s*", "").trim();
                        qwa.optionB = qwa.optionB.replaceAll("^[A-D]\\)\\s*", "").trim();
                        qwa.optionC = qwa.optionC.replaceAll("^[A-D]\\)\\s*", "").trim();
                        qwa.optionD = qwa.optionD.replaceAll("^[A-D]\\)\\s*", "").trim();
                        
                        questions.add(qwa);
                    }
                }
            }
            
            if (questions.isEmpty()) {
                System.err.println("No questions parsed from response. Response was: " + response);
            }
            
            return questions;
            
        } catch (Exception e) {
            System.err.println("Error generating quiz questions with answers: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<QuizAnswer> generateQuizAnswers(String questionText) {
        try {
            // Clean question text - remove any embedded options that might have been added
            String cleanQuestionText = questionText;
            // Remove options that might be embedded in the question
            cleanQuestionText = cleanQuestionText.replaceAll("(?i)\\s*[A-D]\\)\\s*[^\\n]*", "").trim();
            cleanQuestionText = cleanQuestionText.replaceAll("QUESTION\\s*:?\\s*", "").trim();
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an AI assistant that creates multiple choice answers for quiz questions. " +
                "Create 4 answer options for the given question. " +
                "IMPORTANT: Format EXACTLY as: CORRECT_ANSWER|WRONG_ANSWER_1|WRONG_ANSWER_2|WRONG_ANSWER_3 " +
                "Where:\n" +
                "- The first answer is the CORRECT answer\n" +
                "- The other three are plausible but INCORRECT answers\n" +
                "- Each answer should be concise (1-2 sentences max)\n" +
                "- Do NOT include letters (A) B) C) D)) in the answers\n" +
                "Return ONLY the pipe-separated answers, nothing else."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), "Question: " + cleanQuestionText));
            
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
            
            System.out.println("=== ANSWER GENERATION RESPONSE ===");
            System.out.println("Question: " + cleanQuestionText);
            System.out.println("Response: " + response);
            System.out.println("=== END RESPONSE ===");
            
            // Parse the response into quiz answers
            List<QuizAnswer> answers = new ArrayList<>();
            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Skip header lines
                if (line.toLowerCase().startsWith("answer") || 
                    line.toLowerCase().startsWith("option")) {
                    continue;
                }
                
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        // Correct answer (first one)
                        QuizAnswer correctAnswer = new QuizAnswer();
                        String correctText = parts[0].trim();
                        correctText = correctText.replaceAll("^[A-D]\\)\\s*", "").trim();
                        correctAnswer.setAnswerText(correctText);
                        correctAnswer.setIsCorrect(true);
                        answers.add(correctAnswer);
                        
                        // Wrong answers
                        for (int i = 1; i < parts.length && i < 4; i++) {
                            QuizAnswer wrongAnswer = new QuizAnswer();
                            String wrongText = parts[i].trim();
                            wrongText = wrongText.replaceAll("^[A-D]\\)\\s*", "").trim();
                            wrongAnswer.setAnswerText(wrongText);
                            wrongAnswer.setIsCorrect(false);
                            answers.add(wrongAnswer);
                        }
                        break; // Found valid answer set, stop parsing
                    }
                }
            }
            
            // If parsing failed, throw exception instead of using placeholder
            if (answers.isEmpty()) {
                System.err.println("Failed to parse answers from response: " + response);
                throw new IllegalStateException("Failed to generate valid answers for question. AI response was: " + response);
            }
            
            return answers;
            
        } catch (Exception e) {
            System.err.println("Error generating quiz answers: " + e.getMessage());
            e.printStackTrace();
            // Re-throw instead of returning placeholder answers
            throw new RuntimeException("Failed to generate quiz answers: " + e.getMessage(), e);
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
