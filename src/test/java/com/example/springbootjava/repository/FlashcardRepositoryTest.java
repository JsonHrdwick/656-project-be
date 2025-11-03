package com.example.springbootjava.repository;

import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class FlashcardRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Flashcard flashcard1;
    private Flashcard flashcard2;

    @BeforeEach
    void setUp() {
        // Create and persist test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password123");
        testUser.setEnabled(true);
        testUser = entityManager.persistAndFlush(testUser);

        // Create test flashcards
        flashcard1 = new Flashcard();
        flashcard1.setQuestion("What is Spring Boot?");
        flashcard1.setAnswer("A Java framework");
        flashcard1.setUser(testUser);
        flashcard1.setDifficulty(Flashcard.Difficulty.EASY);
        flashcard1.setCategory("Java");

        flashcard2 = new Flashcard();
        flashcard2.setQuestion("What is JPA?");
        flashcard2.setAnswer("Java Persistence API");
        flashcard2.setUser(testUser);
        flashcard2.setDifficulty(Flashcard.Difficulty.MEDIUM);
        flashcard2.setCategory("Java");
    }

    @Test
    void testSaveFlashcard() {
        Flashcard saved = flashcardRepository.save(flashcard1);
        
        assertNotNull(saved.getId());
        assertEquals(flashcard1.getQuestion(), saved.getQuestion());
        assertEquals(flashcard1.getAnswer(), saved.getAnswer());
        assertEquals(testUser.getId(), saved.getUser().getId());
    }

    @Test
    void testFindById() {
        Flashcard saved = entityManager.persistAndFlush(flashcard1);
        
        Optional<Flashcard> found = flashcardRepository.findById(saved.getId());
        
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(saved.getQuestion(), found.get().getQuestion());
    }

    @Test
    void testFindByUser() {
        entityManager.persistAndFlush(flashcard1);
        entityManager.persistAndFlush(flashcard2);
        
        List<Flashcard> flashcards = flashcardRepository.findByUserOrderByCreatedAtDesc(testUser);
        
        assertNotNull(flashcards);
        assertEquals(2, flashcards.size());
        // Should be ordered by createdAt DESC, so flashcard2 should be first
        assertTrue(flashcards.get(0).getCreatedAt().isAfter(flashcards.get(1).getCreatedAt()) ||
                   flashcards.get(0).getCreatedAt().isEqual(flashcards.get(1).getCreatedAt()));
    }

    @Test
    void testFindByUserAndDifficulty() {
        entityManager.persistAndFlush(flashcard1);
        entityManager.persistAndFlush(flashcard2);
        
        List<Flashcard> easyFlashcards = flashcardRepository.findByUserAndDifficulty(
            testUser, Flashcard.Difficulty.EASY);
        
        assertEquals(1, easyFlashcards.size());
        assertEquals(Flashcard.Difficulty.EASY, easyFlashcards.get(0).getDifficulty());
        assertEquals("What is Spring Boot?", easyFlashcards.get(0).getQuestion());
    }

    @Test
    void testFindByUserAndCategory() {
        entityManager.persistAndFlush(flashcard1);
        entityManager.persistAndFlush(flashcard2);
        
        List<Flashcard> javaFlashcards = flashcardRepository.findByUserAndCategory(testUser, "Java");
        
        assertEquals(2, javaFlashcards.size());
        javaFlashcards.forEach(card -> assertEquals("Java", card.getCategory()));
    }

    @Test
    void testCountByUser() {
        entityManager.persistAndFlush(flashcard1);
        entityManager.persistAndFlush(flashcard2);
        
        long count = flashcardRepository.countByUser(testUser);
        
        assertEquals(2, count);
    }

    @Test
    void testCountByUserAndDifficulty() {
        entityManager.persistAndFlush(flashcard1);
        entityManager.persistAndFlush(flashcard2);
        
        long easyCount = flashcardRepository.countByUserAndDifficulty(
            testUser, Flashcard.Difficulty.EASY);
        long mediumCount = flashcardRepository.countByUserAndDifficulty(
            testUser, Flashcard.Difficulty.MEDIUM);
        
        assertEquals(1, easyCount);
        assertEquals(1, mediumCount);
    }

    @Test
    void testDeleteFlashcard() {
        Flashcard saved = entityManager.persistAndFlush(flashcard1);
        Long id = saved.getId();
        
        flashcardRepository.deleteById(id);
        entityManager.flush();
        
        Optional<Flashcard> deleted = flashcardRepository.findById(id);
        assertFalse(deleted.isPresent());
    }
}

