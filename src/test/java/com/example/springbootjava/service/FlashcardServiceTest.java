package com.example.springbootjava.service;

import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.FlashcardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private AIService aiService;

    @Mock
    private DocumentContentExtractor contentExtractor;

    @InjectMocks
    private FlashcardService flashcardService;

    private User testUser;
    private Flashcard testFlashcard;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        testFlashcard = new Flashcard();
        testFlashcard.setId(1L);
        testFlashcard.setQuestion("What is Java?");
        testFlashcard.setAnswer("A programming language");
        testFlashcard.setUser(testUser);
        testFlashcard.setDifficulty(Flashcard.Difficulty.MEDIUM);
        testFlashcard.setCategory("Programming");
    }

    @Test
    void testCreateFlashcard() {
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        Flashcard result = flashcardService.createFlashcard(testFlashcard);

        assertNotNull(result);
        assertEquals(testFlashcard.getId(), result.getId());
        assertEquals(testFlashcard.getQuestion(), result.getQuestion());
        assertEquals(testFlashcard.getAnswer(), result.getAnswer());
        verify(flashcardRepository, times(1)).save(testFlashcard);
    }

    @Test
    void testGetFlashcardById_Found() {
        when(flashcardRepository.findById(1L)).thenReturn(Optional.of(testFlashcard));

        Optional<Flashcard> result = flashcardService.getFlashcardById(1L);

        assertTrue(result.isPresent());
        assertEquals(testFlashcard.getId(), result.get().getId());
        verify(flashcardRepository, times(1)).findById(1L);
    }

    @Test
    void testGetFlashcardById_NotFound() {
        when(flashcardRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Flashcard> result = flashcardService.getFlashcardById(999L);

        assertFalse(result.isPresent());
        verify(flashcardRepository, times(1)).findById(999L);
    }

    @Test
    void testUpdateFlashcard() {
        Flashcard updatedFlashcard = new Flashcard();
        updatedFlashcard.setId(1L);
        updatedFlashcard.setQuestion("What is Java? (Updated)");
        updatedFlashcard.setAnswer("A programming language (Updated)");
        updatedFlashcard.setUser(testUser);

        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(updatedFlashcard);

        Flashcard result = flashcardService.updateFlashcard(updatedFlashcard);

        assertNotNull(result);
        assertEquals(updatedFlashcard.getQuestion(), result.getQuestion());
        verify(flashcardRepository, times(1)).save(updatedFlashcard);
    }

    @Test
    void testDeleteFlashcard() {
        doNothing().when(flashcardRepository).deleteById(1L);

        flashcardService.deleteFlashcard(1L);

        verify(flashcardRepository, times(1)).deleteById(1L);
    }

    @Test
    void testGetFlashcardCount() {
        when(flashcardRepository.countByUser(testUser)).thenReturn(5L);

        long count = flashcardService.getFlashcardCount(testUser);

        assertEquals(5L, count);
        verify(flashcardRepository, times(1)).countByUser(testUser);
    }

    @Test
    void testGetFlashcardCountByDifficulty() {
        when(flashcardRepository.countByUserAndDifficulty(testUser, Flashcard.Difficulty.HARD))
                .thenReturn(2L);

        long count = flashcardService.getFlashcardCountByDifficulty(testUser, Flashcard.Difficulty.HARD);

        assertEquals(2L, count);
        verify(flashcardRepository, times(1)).countByUserAndDifficulty(testUser, Flashcard.Difficulty.HARD);
    }

    @Test
    void testGetUserFlashcards() {
        List<Flashcard> flashcards = Arrays.asList(testFlashcard);
        when(flashcardRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(flashcards);

        List<Flashcard> result = flashcardService.getUserFlashcards(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testFlashcard.getId(), result.get(0).getId());
        verify(flashcardRepository, times(1)).findByUserOrderByCreatedAtDesc(testUser);
    }
}

