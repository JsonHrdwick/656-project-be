package com.example.springbootjava.repository;

import com.example.springbootjava.entity.FlashcardStudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardStudySessionRepository extends JpaRepository<FlashcardStudySession, Long> {
    
    List<FlashcardStudySession> findByFlashcardIdOrderByCreatedAtDesc(Long flashcardId);
    
    @Query("SELECT s FROM FlashcardStudySession s WHERE s.flashcard.id = :flashcardId ORDER BY s.createdAt DESC")
    List<FlashcardStudySession> findByFlashcardIdOrderByCreatedAt(@Param("flashcardId") Long flashcardId);
    
    @Query("SELECT COUNT(s) FROM FlashcardStudySession s WHERE s.flashcard.id = :flashcardId")
    long countByFlashcardId(@Param("flashcardId") Long flashcardId);
    
    // Backup-related methods
    @Query("SELECT s FROM FlashcardStudySession s WHERE s.flashcard.user.id = :userId")
    List<FlashcardStudySession> findByFlashcardUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM FlashcardStudySession s WHERE s.flashcard.user.id = :userId")
    void deleteByFlashcardUserId(@Param("userId") Long userId);
}
