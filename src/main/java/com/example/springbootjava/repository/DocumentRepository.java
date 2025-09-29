package com.example.springbootjava.repository;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByUserOrderByCreatedAtDesc(User user);
    
    Page<Document> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.user = :user AND d.processingStatus = :status ORDER BY d.createdAt DESC")
    List<Document> findByUserAndProcessingStatus(@Param("user") User user, @Param("status") Document.ProcessingStatus status);
    
    @Query("SELECT d FROM Document d WHERE d.user = :user AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(d.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY d.createdAt DESC")
    List<Document> findByUserAndSearchTerm(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT d FROM Document d WHERE d.user = :user AND d.fileType = :fileType ORDER BY d.createdAt DESC")
    List<Document> findByUserAndFileType(@Param("user") User user, @Param("fileType") String fileType);
    
    @Query("SELECT COUNT(d) FROM Document d WHERE d.user = :user")
    long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(d) FROM Document d WHERE d.user = :user AND d.processingStatus = :status")
    long countByUserAndProcessingStatus(@Param("user") User user, @Param("status") Document.ProcessingStatus status);
    
    @Query("SELECT d FROM Document d WHERE d.processingStatus = :status ORDER BY d.createdAt ASC")
    List<Document> findByProcessingStatusOrderByCreatedAt(@Param("status") Document.ProcessingStatus status);
}
