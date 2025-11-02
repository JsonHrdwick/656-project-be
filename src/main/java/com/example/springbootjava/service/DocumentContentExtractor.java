package com.example.springbootjava.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DocumentContentExtractor {
    
    private final Tika tika = new Tika();
    
    @Value("${document.storage.local.base-path:./uploads}")
    private String basePath;
    
    @Autowired
    private LocalFileStorageService fileStorageService;
    
    public String extractContent(String filePath) throws IOException {
        System.out.println("=== CONTENT EXTRACTION START ===");
        System.out.println("Extracting content from filePath: " + filePath);
        System.out.println("Base path: " + basePath);
        
        try {
            // Check if it's a mock path
            if (filePath.startsWith("mock://")) {
                System.out.println("Mock file detected, generating placeholder content");
                String extension = getFileExtension(filePath);
                return generatePlaceholderContent(extension, filePath);
            }
            
            // Check if file exists using the storage service
            System.out.println("Checking if file exists...");
            String pathForCheck = filePath.startsWith("uploads/") ? filePath.substring(7) : filePath;
            if (!fileStorageService.fileExists(pathForCheck)) {
                System.err.println("File does not exist: " + filePath);
                String extension = getFileExtension(filePath);
                return generatePlaceholderContent(extension, filePath);
            }
            System.out.println("File exists, proceeding with extraction");
            
            // Build full path using base path
            Path fullPath;
            if (filePath.startsWith("uploads/")) {
                // File path already includes uploads prefix, use it directly
                fullPath = Paths.get(basePath, filePath.substring(7)); // Remove "uploads/" prefix
            } else {
                // File path doesn't include uploads prefix, combine with base path
                fullPath = Paths.get(basePath, filePath);
            }
            System.out.println("Full path for extraction: " + fullPath.toString());
            System.out.println("File exists at full path: " + Files.exists(fullPath));
            
            // Use Apache Tika to extract content from all supported file types
            System.out.println("Starting Tika content extraction...");
            String content = tika.parseToString(fullPath);
            
            // Clean up the content
            if (content != null && !content.trim().isEmpty()) {
                System.out.println("Successfully extracted content, length: " + content.length());
                System.out.println("Content preview: " + content.substring(0, Math.min(200, content.length())) + "...");
                return content.trim();
            } else {
                // Fallback to placeholder if Tika couldn't extract content
                System.out.println("Tika returned empty content, using placeholder");
                String extension = getFileExtension(filePath);
                return generatePlaceholderContent(extension, Paths.get(filePath).getFileName().toString());
            }
            
        } catch (TikaException e) {
            System.err.println("Tika parsing failed for file: " + filePath + ", error: " + e.getMessage());
            e.printStackTrace();
            // Fallback to placeholder content
            String extension = getFileExtension(filePath);
            return generatePlaceholderContent(extension, Paths.get(filePath).getFileName().toString());
        } catch (Exception e) {
            System.err.println("Unexpected error during content extraction: " + e.getMessage());
            e.printStackTrace();
            String extension = getFileExtension(filePath);
            return generatePlaceholderContent(extension, Paths.get(filePath).getFileName().toString());
        } finally {
            System.out.println("=== CONTENT EXTRACTION END ===");
        }
    }
    
    private String extractTextContent(Path path) throws IOException {
        return Files.readString(path);
    }
    
    private String generatePlaceholderContent(String fileType, String filename) {
        StringBuilder content = new StringBuilder();
        
        content.append("Document: ").append(filename).append("\n");
        content.append("Type: ").append(fileType.toUpperCase()).append("\n\n");
        
        content.append("This is placeholder content for a ").append(fileType).append(" file.\n");
        content.append("In a production environment, this would be extracted using Apache Tika or similar libraries.\n\n");
        
        content.append("Sample content for demonstration:\n");
        content.append("This document contains important information that would normally be extracted from the actual file.\n");
        content.append("The content extraction process would handle various file formats including PDF, Word documents, PowerPoint presentations, and more.\n\n");
        
        content.append("Key topics that might be covered:\n");
        content.append("- Technical specifications\n");
        content.append("- Project requirements\n");
        content.append("- Implementation details\n");
        content.append("- Best practices and guidelines\n\n");
        
        content.append("Note: This is simulated content for demonstration purposes.\n");
        content.append("Actual content extraction would require additional dependencies and configuration.");
        
        return content.toString();
    }
    
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1).toLowerCase();
    }
}