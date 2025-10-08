package com.example.springbootjava.repository;

import com.example.springbootjava.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    
    List<QuizQuestion> findByQuizIdOrderByOrderAsc(Long quizId);
    
    @Query("SELECT q FROM QuizQuestion q WHERE q.quiz.id = :quizId ORDER BY q.order ASC")
    List<QuizQuestion> findByQuizIdOrderByOrder(@Param("quizId") Long quizId);
    
    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    long countByQuizId(@Param("quizId") Long quizId);
    
    // Backup-related methods
    @Query("SELECT q FROM QuizQuestion q WHERE q.quiz.user.id = :userId")
    List<QuizQuestion> findByQuizUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM QuizQuestion q WHERE q.quiz.user.id = :userId")
    void deleteByQuizUserId(@Param("userId") Long userId);
}
