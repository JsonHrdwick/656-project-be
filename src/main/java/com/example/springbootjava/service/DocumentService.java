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
    
    @Value("${document.processing.simulate:false}")
    private boolean simulateProcessing;
    
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
        
        // Store file locally
        System.out.println("Storing file locally...");
        String storedFilePath;
        if (localStorageEnabled) {
            storedFilePath = fileStorageService.storeFile(file, user.getId());
            System.out.println("File stored at: " + storedFilePath);
        } else {
            // Fallback for when local storage is disabled
            storedFilePath = "mock://" + originalFilename;
            System.out.println("Local storage disabled, using mock path: " + storedFilePath);
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
                } else {
                    System.out.println("Skipping file deletion (local storage disabled or mock file)");
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
                System.out.println("=== SIMULATED DOCUMENT PROCESSING START ===");
                System.out.println("Processing document ID: " + document.getId());
                System.out.println("Document title: " + document.getTitle());
                
                // Extract content from file
                System.out.println("Extracting content from file...");
                String content;
                if (simulateProcessing) {
                    // Simulate content extraction (generate sample content based on file type)
                    System.out.println("Simulating content extraction...");
                    content = generateSimulatedContent(document.getFileType(), document.getTitle());
                } else if (localStorageEnabled && document.getFilePath() != null && !document.getFilePath().startsWith("mock://")) {
                    // Extract real content from stored file
                    System.out.println("Extracting real content from file: " + document.getFilePath());
                    try {
                        content = contentExtractor.extractContent(document.getFilePath());
                    } catch (IOException e) {
                        System.out.println("Error extracting content, falling back to simulation: " + e.getMessage());
                        content = generateSimulatedContent(document.getFileType(), document.getTitle());
                    }
                } else {
                    // Fallback to simulation
                    System.out.println("Using simulated content (local storage disabled or mock file)");
                    content = generateSimulatedContent(document.getFileType(), document.getTitle());
                }
                
                document.setContent(content);
                System.out.println("Content extracted, length: " + content.length());
                
                // Generate AI summary
                System.out.println("Generating AI summary...");
                String summary = aiService.generateSummary(content);
                document.setSummary(summary);
                System.out.println("AI summary generated: " + summary);
                
                // Update processing status
                System.out.println("Updating processing status...");
                document.setProcessingStatus(Document.ProcessingStatus.COMPLETED);
                documentRepository.save(document);
                System.out.println("Document processing completed successfully");
                System.out.println("=== SIMULATED DOCUMENT PROCESSING END ===");
                
            } catch (Exception e) {
                System.out.println("=== DOCUMENT PROCESSING ERROR ===");
                System.out.println("Error processing document: " + e.getMessage());
                e.printStackTrace();
                System.out.println("=== DOCUMENT PROCESSING ERROR END ===");
                
                try {
                    document.setProcessingStatus(Document.ProcessingStatus.FAILED);
                    documentRepository.save(document);
                } catch (Exception saveError) {
                    System.out.println("Failed to save error status: " + saveError.getMessage());
                }
            }
        });
    }
    
    private String generateSimulatedContent(String fileType, String title) {
        // Generate realistic sample content based on file type for POC demonstration
        StringBuilder content = new StringBuilder();
        
        content.append("Document Title: ").append(title).append("\n\n");
        content.append("File Type: ").append(fileType).append("\n\n");
        
        switch (fileType.toLowerCase()) {
            case "pdf":
                content.append("This is a simulated PDF document content for demonstration purposes.\n\n");
                content.append("Chapter 1: Introduction\n");
                content.append("This document contains important information about the topic. ");
                content.append("The content would normally be extracted from the actual PDF file using Apache Tika or similar libraries.\n\n");
                content.append("Chapter 2: Main Concepts\n");
                content.append("Key concepts include data structures, algorithms, and system design principles. ");
                content.append("These concepts are fundamental to understanding the subject matter.\n\n");
                content.append("Chapter 3: Implementation\n");
                content.append("The implementation details cover various approaches and best practices. ");
                content.append("This section provides practical guidance for developers.\n\n");
                break;
                
            case "docx":
            case "doc":
                content.append("This is a simulated Word document content for demonstration purposes.\n\n");
                content.append("Executive Summary\n");
                content.append("This document outlines the key findings and recommendations from our analysis. ");
                content.append("The content demonstrates how AI can process and understand document structure.\n\n");
                content.append("Key Findings\n");
                content.append("1. Performance improvements of 40% were achieved\n");
                content.append("2. User satisfaction increased by 25%\n");
                content.append("3. System reliability improved significantly\n\n");
                break;
                
            case "txt":
                content.append("This is a simulated text document content for demonstration purposes.\n\n");
                content.append("Project Overview\n");
                content.append("This text file contains important project information and notes. ");
                content.append("The content is structured to demonstrate AI processing capabilities.\n\n");
                content.append("Technical Specifications\n");
                content.append("- Framework: Spring Boot\n");
                content.append("- Database: PostgreSQL\n");
                content.append("- Frontend: React/Next.js\n");
                content.append("- Deployment: Railway\n\n");
                break;
                
            default:
                content.append("This is simulated content for a ").append(fileType).append(" file.\n\n");
                content.append("The content demonstrates how the AI service can process different file types ");
                content.append("and generate summaries, flashcards, and quiz questions based on the document content.\n\n");
                content.append("Key Topics Covered:\n");
                content.append("- Document processing and analysis\n");
                content.append("- AI-powered content generation\n");
                content.append("- Educational tool development\n");
                content.append("- User experience optimization\n\n");
                break;
        }
        
        content.append("Note: This is simulated content for POC demonstration. ");
        content.append("In a production environment, actual file content would be extracted and processed.");
        
        return content.toString();
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
