package com.example.springbootjava.repository;

import com.example.springbootjava.entity.Quiz;
import com.example.springbootjava.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    
    List<Quiz> findByUserOrderByCreatedAtDesc(User user);
    
    Page<Quiz> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Query("SELECT q FROM Quiz q WHERE q.user = :user AND q.isPublished = true ORDER BY q.createdAt DESC")
    List<Quiz> findPublishedByUser(@Param("user") User user);
    
    @Query("SELECT q FROM Quiz q WHERE q.user = :user AND q.difficulty = :difficulty ORDER BY q.createdAt DESC")
    List<Quiz> findByUserAndDifficulty(@Param("user") User user, @Param("difficulty") Quiz.Difficulty difficulty);
    
    @Query("SELECT q FROM Quiz q WHERE q.user = :user AND (LOWER(q.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(q.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY q.createdAt DESC")
    List<Quiz> findByUserAndSearchTerm(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.user = :user")
    long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.user = :user AND q.isPublished = true")
    long countPublishedByUser(@Param("user") User user);
    
    @Query("SELECT q FROM Quiz q WHERE q.isPublished = true ORDER BY q.createdAt DESC")
    List<Quiz> findAllPublished();
}
