package com.example.springbootjava.service;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.Quiz;
import com.example.springbootjava.entity.QuizAnswer;
import com.example.springbootjava.entity.QuizQuestion;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.QuizRepository;
import com.example.springbootjava.repository.QuizQuestionRepository;
import com.example.springbootjava.repository.QuizAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private QuizAnswerRepository quizAnswerRepository;

    @Mock
    private AIService aiService;

    @Mock
    private DocumentContentExtractor contentExtractor;

    @InjectMocks
    private QuizService quizService;

    private User testUser;
    private Quiz testQuiz;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setTitle("Test Document");
        testDocument.setFileType("PDF");
        testDocument.setFilePath("mock://test.pdf");

        testQuiz = new Quiz();
        testQuiz.setId(1L);
        testQuiz.setTitle("Test Quiz");
        testQuiz.setDescription("A test quiz");
        testQuiz.setTimeLimitMinutes(10);
        testQuiz.setDifficulty(Quiz.Difficulty.MEDIUM);
        testQuiz.setIsPublished(true);
        testQuiz.setUser(testUser);
        testQuiz.setDocument(testDocument);
    }

    @Test
    void testCreateQuiz() {
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);

        Quiz result = quizService.createQuiz(testQuiz);

        assertNotNull(result);
        assertEquals(testQuiz.getId(), result.getId());
        assertEquals(testQuiz.getTitle(), result.getTitle());
        verify(quizRepository, times(1)).save(testQuiz);
    }

    @Test
    void testGetQuizById_Found() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(testQuiz));

        Optional<Quiz> result = quizService.getQuizById(1L);

        assertTrue(result.isPresent());
        assertEquals(testQuiz.getId(), result.get().getId());
        assertEquals(testQuiz.getTitle(), result.get().getTitle());
        verify(quizRepository, times(1)).findById(1L);
    }

    @Test
    void testGetQuizById_NotFound() {
        when(quizRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Quiz> result = quizService.getQuizById(999L);

        assertFalse(result.isPresent());
        verify(quizRepository, times(1)).findById(999L);
    }

    @Test
    void testUpdateQuiz() {
        Quiz updatedQuiz = new Quiz();
        updatedQuiz.setId(1L);
        updatedQuiz.setTitle("Updated Quiz");
        updatedQuiz.setDescription("Updated description");
        updatedQuiz.setUser(testUser);

        when(quizRepository.save(any(Quiz.class))).thenReturn(updatedQuiz);

        Quiz result = quizService.updateQuiz(updatedQuiz);

        assertNotNull(result);
        assertEquals(updatedQuiz.getTitle(), result.getTitle());
        verify(quizRepository, times(1)).save(updatedQuiz);
    }

    @Test
    void testDeleteQuiz() {
        doNothing().when(quizRepository).deleteById(1L);

        quizService.deleteQuiz(1L);

        verify(quizRepository, times(1)).deleteById(1L);
    }

    @Test
    void testGetUserQuizzes() {
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        when(quizRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(quizzes);

        List<Quiz> result = quizService.getUserQuizzes(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testQuiz.getId(), result.get(0).getId());
        verify(quizRepository, times(1)).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    void testGetUserQuizzesWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Quiz> quizPage = new PageImpl<>(Arrays.asList(testQuiz), pageable, 1);
        when(quizRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(quizPage);

        Page<Quiz> result = quizService.getUserQuizzes(testUser, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        verify(quizRepository, times(1)).findByUserOrderByCreatedAtDesc(testUser, pageable);
    }

    @Test
    void testGetPublishedQuizzes() {
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        when(quizRepository.findPublishedByUser(testUser)).thenReturn(quizzes);

        List<Quiz> result = quizService.getPublishedQuizzes(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(quizRepository, times(1)).findPublishedByUser(testUser);
    }

    @Test
    void testGetQuizzesByDifficulty() {
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        when(quizRepository.findByUserAndDifficulty(testUser, Quiz.Difficulty.MEDIUM)).thenReturn(quizzes);

        List<Quiz> result = quizService.getQuizzesByDifficulty(testUser, Quiz.Difficulty.MEDIUM);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Quiz.Difficulty.MEDIUM, result.get(0).getDifficulty());
        verify(quizRepository, times(1)).findByUserAndDifficulty(testUser, Quiz.Difficulty.MEDIUM);
    }

    @Test
    void testSearchQuizzes() {
        String searchTerm = "test";
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        when(quizRepository.findByUserAndSearchTerm(testUser, searchTerm)).thenReturn(quizzes);

        List<Quiz> result = quizService.searchQuizzes(testUser, searchTerm);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(quizRepository, times(1)).findByUserAndSearchTerm(testUser, searchTerm);
    }

    @Test
    void testGetQuizCount() {
        when(quizRepository.countByUser(testUser)).thenReturn(5L);

        long count = quizService.getQuizCount(testUser);

        assertEquals(5L, count);
        verify(quizRepository, times(1)).countByUser(testUser);
    }

    @Test
    void testGetPublishedQuizCount() {
        when(quizRepository.countPublishedByUser(testUser)).thenReturn(3L);

        long count = quizService.getPublishedQuizCount(testUser);

        assertEquals(3L, count);
        verify(quizRepository, times(1)).countPublishedByUser(testUser);
    }

    @Test
    void testGenerateQuizFromText() {
        String text = "This is sample content for generating a quiz.";
        String title = "Sample Quiz";
        int numberOfQuestions = 3;

        // Create mock quiz questions
        QuizQuestion question1 = new QuizQuestion();
        question1.setQuestionText("Question 1?");
        question1.setQuestionType(QuizQuestion.QuestionType.MULTIPLE_CHOICE);
        question1.setPoints(1);

        QuizQuestion question2 = new QuizQuestion();
        question2.setQuestionText("Question 2?");
        question2.setQuestionType(QuizQuestion.QuestionType.MULTIPLE_CHOICE);
        question2.setPoints(1);

        List<QuizQuestion> mockQuestions = Arrays.asList(question1, question2);

        // Create mock answers
        QuizAnswer answer1 = new QuizAnswer();
        answer1.setAnswerText("Answer 1");
        answer1.setIsCorrect(true);

        QuizAnswer answer2 = new QuizAnswer();
        answer2.setAnswerText("Answer 2");
        answer2.setIsCorrect(false);

        List<QuizAnswer> mockAnswers = Arrays.asList(answer1, answer2);

        // First save returns the quiz with ID, second save returns the same quiz
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(1L); // Set ID for the saved quiz
            return quiz;
        });
        when(aiService.generateQuizQuestions(anyString(), anyString(), anyInt())).thenReturn(mockQuestions);
        when(quizQuestionRepository.save(any(QuizQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiService.generateQuizAnswers(anyString())).thenReturn(mockAnswers);
        when(quizAnswerRepository.save(any(QuizAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quiz result = quizService.generateQuizFromText(text, title, testUser, numberOfQuestions);

        assertNotNull(result);
        assertNotNull(result.getTitle());
        assertTrue(result.getTitle().contains(title), "Quiz title should contain: " + title + ", but was: " + result.getTitle());
        assertEquals(testUser, result.getUser());
        verify(quizRepository, atLeastOnce()).save(any(Quiz.class));
        verify(aiService, times(1)).generateQuizQuestions(text, title, numberOfQuestions);
    }
}

