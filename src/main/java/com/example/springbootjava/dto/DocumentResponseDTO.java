package com.example.springbootjava.dto;

import com.example.springbootjava.entity.Document;
import java.time.LocalDateTime;

public class DocumentResponseDTO {
    private Long id;
    private String title;
    private String fileType;
    private String originalFilename;
    private String filePath;
    private Long fileSize;
    private String content;
    private String summary;
    private Document.ProcessingStatus processingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userEmail; // Just the email, not the full user object

    public DocumentResponseDTO() {}

    public DocumentResponseDTO(Document document) {
        this.id = document.getId();
        this.title = document.getTitle();
        this.fileType = document.getFileType();
        this.originalFilename = document.getFileName();
        this.filePath = document.getFilePath();
        this.fileSize = document.getFileSize();
        this.content = document.getContent();
        this.summary = document.getSummary();
        this.processingStatus = document.getProcessingStatus();
        this.createdAt = document.getCreatedAt();
        this.updatedAt = document.getUpdatedAt();
        this.userEmail = document.getUser() != null ? document.getUser().getEmail() : null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Document.ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(Document.ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
