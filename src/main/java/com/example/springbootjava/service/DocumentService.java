package com.example.springbootjava.service;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private AIService aiService;
    
    private final String uploadDir = "uploads/documents";
    
    public Document uploadDocument(MultipartFile file, User user) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFilename);
        
        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Create document entity
        Document document = new Document(
                originalFilename,
                getFileType(originalFilename),
                originalFilename,
                filePath.toString(),
                file.getSize(),
                user
        );
        
        // Save document
        document = documentRepository.save(document);
        
        // Process document asynchronously
        processDocumentAsync(document);
        
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
                // Delete file from filesystem
                try {
                    Files.deleteIfExists(Paths.get(document.getFilePath()));
                } catch (IOException e) {
                    // Log error but continue with database deletion
                }
                
                documentRepository.delete(document);
            }
        }
    }
    
    private void processDocumentAsync(Document document) {
        // In a real application, this would be processed asynchronously
        // For now, we'll process it synchronously
        try {
            // Extract text content from file
            String content = extractTextContent(document.getFilePath());
            document.setContent(content);
            
            // Generate summary
            String summary = aiService.generateSummary(content);
            document.setSummary(summary);
            
            // Update processing status
            document.setProcessingStatus(Document.ProcessingStatus.COMPLETED);
            documentRepository.save(document);
            
        } catch (Exception e) {
            document.setProcessingStatus(Document.ProcessingStatus.FAILED);
            documentRepository.save(document);
        }
    }
    
    private String extractTextContent(String filePath) throws IOException {
        // This is a simplified implementation
        // In production, you would use Apache Tika or similar library
        Path path = Paths.get(filePath);
        return Files.readString(path);
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
