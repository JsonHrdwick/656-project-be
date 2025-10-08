package com.example.springbootjava.repository;

import com.example.springbootjava.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, Long> {
    
    List<QuizAttemptAnswer> findByAttemptId(Long attemptId);
    
    List<QuizAttemptAnswer> findByQuestionId(Long questionId);
    
    @Query("SELECT a FROM QuizAttemptAnswer a WHERE a.attempt.id = :attemptId AND a.question.id = :questionId")
    QuizAttemptAnswer findByAttemptIdAndQuestionId(@Param("attemptId") Long attemptId, @Param("questionId") Long questionId);
    
    @Query("SELECT COUNT(a) FROM QuizAttemptAnswer a WHERE a.attempt.id = :attemptId")
    long countByAttemptId(@Param("attemptId") Long attemptId);
    
    // Backup-related methods
    @Query("SELECT a FROM QuizAttemptAnswer a WHERE a.attempt.user.id = :userId")
    List<QuizAttemptAnswer> findByAttemptUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM QuizAttemptAnswer a WHERE a.attempt.user.id = :userId")
    void deleteByAttemptUserId(@Param("userId") Long userId);
}
