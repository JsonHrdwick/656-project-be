package com.example.springbootjava.service;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class DocumentService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private LocalFileStorageService fileStorageService;
    
    @Autowired
    private DocumentContentExtractor contentExtractor;
    
    @Value("${document.storage.local.enabled:true}")
    private boolean localStorageEnabled;
    
    public Document uploadDocument(MultipartFile file, User user) throws IOException {
        System.out.println("=== DOCUMENT UPLOAD START ===");
        
        // Get file details
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();
        
        System.out.println("Original filename: " + originalFilename);
        System.out.println("Content type: " + contentType);
        System.out.println("File size: " + fileSize + " bytes");
        
        // Store file locally - REQUIRED, no fallbacks
        System.out.println("Storing file locally...");
        System.out.println("Local storage enabled: " + localStorageEnabled);
        
        if (!localStorageEnabled) {
            throw new IllegalStateException("Local storage is disabled. Cannot upload documents without file storage enabled.");
        }
        
        String storedFilePath;
        try {
            storedFilePath = fileStorageService.storeFile(file, user.getId());
            System.out.println("File stored successfully at: " + storedFilePath);
            
            // Verify file was actually stored
            if (!fileStorageService.fileExists(storedFilePath)) {
                throw new IOException("File storage verification failed. File not found at: " + storedFilePath);
            }
            System.out.println("File verification: File exists in storage");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to store file: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to store uploaded file: " + e.getMessage(), e);
        }
        
        // Create document entity
        System.out.println("Creating document entity...");
        Document document = new Document(
                originalFilename,
                getFileType(originalFilename),
                originalFilename,
                storedFilePath, // Store actual file path
                fileSize,
                user
        );
        System.out.println("Document entity created");
        
        // Save document
        System.out.println("Saving document to database...");
        document = documentRepository.save(document);
        System.out.println("Document saved with ID: " + document.getId());
        
        // Process document asynchronously
        System.out.println("Starting document processing...");
        processDocumentAsync(document);
        System.out.println("=== DOCUMENT UPLOAD COMPLETE ===");
        
        return document;
    }
    
    public List<Document> getUserDocuments(User user) {
        return documentRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Page<Document> getUserDocuments(User user, Pageable pageable) {
        return documentRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }
    
    public List<Document> searchDocuments(User user, String searchTerm) {
        return documentRepository.findByUserAndSearchTerm(user, searchTerm);
    }
    
    public List<Document> getDocumentsByFileType(User user, String fileType) {
        return documentRepository.findByUserAndFileType(user, fileType);
    }
    
    public long getDocumentCount(User user) {
        return documentRepository.countByUser(user);
    }
    
    public long getProcessedDocumentCount(User user) {
        return documentRepository.countByUserAndProcessingStatus(user, Document.ProcessingStatus.COMPLETED);
    }
    
    public void deleteDocument(Long id, User user) {
        Optional<Document> documentOpt = documentRepository.findById(id);
        if (documentOpt.isPresent()) {
            Document document = documentOpt.get();
            if (document.getUser().getId().equals(user.getId())) {
                // Delete file from storage if local storage is enabled
                if (localStorageEnabled && document.getFilePath() != null && !document.getFilePath().startsWith("mock://")) {
                    System.out.println("Deleting file from storage: " + document.getFilePath());
                    boolean fileDeleted = fileStorageService.deleteFile(document.getFilePath());
                    if (fileDeleted) {
                        System.out.println("File deleted successfully");
                    } else {
                        System.out.println("Warning: File deletion failed, but continuing with database cleanup");
                    }
                } else if (document.getFilePath() != null && document.getFilePath().startsWith("mock://")) {
                    System.out.println("Skipping file deletion (mock file path detected)");
                }
                
                // Delete from database
                System.out.println("Deleting document from database");
                documentRepository.delete(document);
            }
        }
    }
    
    @Async
    public CompletableFuture<Void> processDocumentAsync(Document document) {
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("=== DOCUMENT PROCESSING START ===");
                System.out.println("Processing document ID: " + document.getId());
                System.out.println("Document title: " + document.getTitle());
                System.out.println("Local storage enabled: " + localStorageEnabled);
                System.out.println("File path: " + document.getFilePath());
                
                // Set status to PROCESSING
                document.setProcessingStatus(Document.ProcessingStatus.PROCESSING);
                documentRepository.save(document);
                System.out.println("Document status set to PROCESSING");
                
                // Validate file path - must not be mock
                if (document.getFilePath() == null || document.getFilePath().startsWith("mock://")) {
                    throw new IOException("Invalid file path: " + document.getFilePath() + ". File must be stored locally.");
                }
                
                // Validate local storage is enabled
                if (!localStorageEnabled) {
                    throw new IllegalStateException("Local storage is disabled. Cannot process document without file storage.");
                }
                
                // Validate file exists
                if (!fileStorageService.fileExists(document.getFilePath())) {
                    throw new IOException("File does not exist at path: " + document.getFilePath());
                }
                
                // Extract content from file for AI processing
                System.out.println("Extracting content from file for AI processing...");
                String contentForAI = contentExtractor.extractContent(document.getFilePath());
                
                if (contentForAI == null || contentForAI.trim().isEmpty()) {
                    throw new IOException("Failed to extract content from file. Content is empty.");
                }
                
                System.out.println("Content extracted successfully, length: " + contentForAI.length());
                
                // Generate AI summary using extracted content
                System.out.println("Generating AI summary using OpenAI...");
                String summary = aiService.generateSummary(contentForAI);
                
                if (summary == null || summary.trim().isEmpty()) {
                    throw new IllegalStateException("AI service failed to generate summary.");
                }
                
                document.setSummary(summary);
                System.out.println("AI summary generated successfully");
                
                // Update processing status to COMPLETED
                System.out.println("Updating processing status to COMPLETED...");
                document.setProcessingStatus(Document.ProcessingStatus.COMPLETED);
                documentRepository.save(document);
                System.out.println("Document processing completed successfully");
                System.out.println("=== DOCUMENT PROCESSING END ===");
                
            } catch (Exception e) {
                System.out.println("=== DOCUMENT PROCESSING ERROR ===");
                System.out.println("Error processing document: " + e.getMessage());
                e.printStackTrace();
                System.out.println("=== DOCUMENT PROCESSING ERROR END ===");
                
                try {
                    document.setProcessingStatus(Document.ProcessingStatus.FAILED);
                    documentRepository.save(document);
                    System.out.println("Document status set to FAILED");
                } catch (Exception saveError) {
                    System.err.println("CRITICAL: Failed to save error status: " + saveError.getMessage());
                    saveError.printStackTrace();
                }
            }
        });
    }
    
    
    private String getFileType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "PDF";
            case "doc":
            case "docx":
                return "DOCX";
            case "txt":
                return "TXT";
            case "ppt":
            case "pptx":
                return "PPTX";
            default:
                return "UNKNOWN";
        }
    }
}
