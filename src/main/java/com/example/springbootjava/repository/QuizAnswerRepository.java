package com.example.springbootjava.repository;

import com.example.springbootjava.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    
    List<QuizAnswer> findByQuestionIdOrderByOrderAsc(Long questionId);
    
    @Query("SELECT a FROM QuizAnswer a WHERE a.question.id = :questionId ORDER BY a.order ASC")
    List<QuizAnswer> findByQuestionIdOrderByOrder(@Param("questionId") Long questionId);
    
    @Query("SELECT COUNT(a) FROM QuizAnswer a WHERE a.question.id = :questionId")
    long countByQuestionId(@Param("questionId") Long questionId);
    
    // Backup-related methods
    @Query("SELECT a FROM QuizAnswer a WHERE a.question.quiz.user.id = :userId")
    List<QuizAnswer> findByQuestionQuizUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM QuizAnswer a WHERE a.question.quiz.user.id = :userId")
    void deleteByQuestionQuizUserId(@Param("userId") Long userId);
}
