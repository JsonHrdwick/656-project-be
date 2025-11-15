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
            // Validate file path - must not be mock
            if (filePath == null || filePath.startsWith("mock://")) {
                throw new IOException("Invalid file path: " + filePath + ". Mock files are not supported. File must be stored locally.");
            }
            
            // Normalize path separators (handle both Windows \ and Unix /)
            String normalizedPath = filePath.replace('\\', '/');
            System.out.println("Normalized path: " + normalizedPath);
            
            // Remove "uploads/" prefix if present (normalized to forward slashes)
            String pathForCheck;
            if (normalizedPath.startsWith("uploads/")) {
                pathForCheck = normalizedPath.substring(8); // Remove "uploads/" prefix (8 chars)
            } else {
                pathForCheck = normalizedPath;
            }
            System.out.println("Path for check: " + pathForCheck);
            
            // Check if file exists using the storage service
            System.out.println("Checking if file exists...");
            if (!fileStorageService.fileExists(pathForCheck)) {
                // Try with the full path as stored
                if (!fileStorageService.fileExists(normalizedPath)) {
                    throw new IOException("File does not exist at path: " + filePath + " (checked: " + pathForCheck + " and " + normalizedPath + ")");
                } else {
                    pathForCheck = normalizedPath;
                }
            }
            System.out.println("File exists, proceeding with extraction");
            
            // Build full path using base path - Paths.get handles path separators correctly
            Path fullPath = Paths.get(basePath, pathForCheck);
            System.out.println("Full path for extraction: " + fullPath.toString());
            System.out.println("File exists at full path: " + Files.exists(fullPath));
            
            if (!Files.exists(fullPath)) {
                throw new IOException("File does not exist at full path: " + fullPath.toString());
            }
            
            // Use Apache Tika to extract content from all supported file types
            System.out.println("Starting Tika content extraction...");
            String content = tika.parseToString(fullPath);
            
            // Clean up the content
            if (content == null || content.trim().isEmpty()) {
                throw new IOException("Tika returned empty content for file: " + filePath + ". Cannot process empty document.");
            }
            
            System.out.println("Successfully extracted content, length: " + content.length());
            System.out.println("Content preview: " + content.substring(0, Math.min(200, content.length())) + "...");
            return content.trim();
            
        } catch (TikaException e) {
            System.err.println("Tika parsing failed for file: " + filePath + ", error: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to extract content from file using Tika: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error during content extraction: " + e.getMessage());
            e.printStackTrace();
            if (e instanceof IOException) {
                throw e;
            }
            throw new IOException("Failed to extract content from file: " + e.getMessage(), e);
        } finally {
            System.out.println("=== CONTENT EXTRACTION END ===");
        }
    }
    
    private String extractTextContent(Path path) throws IOException {
        return Files.readString(path);
    }
    
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1).toLowerCase();
    }
}