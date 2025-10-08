package com.example.springbootjava.repository;

import com.example.springbootjava.entity.QuizAttempt;
import com.example.springbootjava.entity.Quiz;
import com.example.springbootjava.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    
    List<QuizAttempt> findByUserOrderByCompletedAtDesc(User user);
    
    List<QuizAttempt> findByQuizOrderByCompletedAtDesc(Quiz quiz);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user = :user AND qa.quiz = :quiz ORDER BY qa.completedAt DESC")
    List<QuizAttempt> findByUserAndQuiz(@Param("user") User user, @Param("quiz") Quiz quiz);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user = :user AND qa.quiz = :quiz ORDER BY qa.score DESC LIMIT 1")
    Optional<QuizAttempt> findBestAttemptByUserAndQuiz(@Param("user") User user, @Param("quiz") Quiz quiz);
    
    @Query("SELECT AVG(qa.score) FROM QuizAttempt qa WHERE qa.user = :user")
    Double findAverageScoreByUser(@Param("user") User user);
    
    @Query("SELECT AVG(qa.score) FROM QuizAttempt qa WHERE qa.quiz = :quiz")
    Double findAverageScoreByQuiz(@Param("quiz") Quiz quiz);
    
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.user = :user")
    long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.quiz = :quiz")
    long countByQuiz(@Param("quiz") Quiz quiz);
    
    // Backup-related methods
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId")
    List<QuizAttempt> findByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM QuizAttempt qa WHERE qa.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
