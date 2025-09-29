package com.example.springbootjava.repository;

import com.example.springbootjava.entity.Flashcard;
import com.example.springbootjava.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    
    List<Flashcard> findByUserOrderByCreatedAtDesc(User user);
    
    Page<Flashcard> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Query("SELECT f FROM Flashcard f WHERE f.user = :user AND f.difficulty = :difficulty ORDER BY f.createdAt DESC")
    List<Flashcard> findByUserAndDifficulty(@Param("user") User user, @Param("difficulty") Flashcard.Difficulty difficulty);
    
    @Query("SELECT f FROM Flashcard f WHERE f.user = :user AND f.category = :category ORDER BY f.createdAt DESC")
    List<Flashcard> findByUserAndCategory(@Param("user") User user, @Param("category") String category);
    
    @Query("SELECT f FROM Flashcard f WHERE f.user = :user AND (LOWER(f.question) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(f.answer) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY f.createdAt DESC")
    List<Flashcard> findByUserAndSearchTerm(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(f) FROM Flashcard f WHERE f.user = :user")
    long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(f) FROM Flashcard f WHERE f.user = :user AND f.difficulty = :difficulty")
    long countByUserAndDifficulty(@Param("user") User user, @Param("difficulty") Flashcard.Difficulty difficulty);
    
    @Query("SELECT f FROM Flashcard f WHERE f.user = :user ORDER BY RANDOM() LIMIT :limit")
    List<Flashcard> findRandomByUser(@Param("user") User user, @Param("limit") int limit);
}
