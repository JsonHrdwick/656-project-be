package com.example.springbootjava.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageService {
    
    @Value("${document.storage.local.base-path:./uploads}")
    private String basePath;
    
    @Value("${document.storage.local.max-file-size:10MB}")
    private String maxFileSize;
    
    @Value("${document.storage.local.allowed-extensions:pdf,doc,docx,txt,ppt,pptx}")
    private String allowedExtensions;
    
    private static final List<String> ALLOWED_EXTENSIONS_LIST = Arrays.asList(
        "pdf", "doc", "docx", "txt", "ppt", "pptx"
    );
    
    public String storeFile(MultipartFile file, Long userId) throws IOException {
        // Validate file
        validateFile(file);
        
        // Create user-specific directory
        Path userDir = createUserDirectory(userId);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = generateUniqueFilename(originalFilename, extension);
        
        // Store file
        Path filePath = userDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path for database storage
        return Paths.get("uploads", "user_" + userId, uniqueFilename).toString();
    }
    
    public byte[] retrieveFile(String filePath) throws IOException {
        Path fullPath = Paths.get(basePath, filePath);
        if (!Files.exists(fullPath)) {
            throw new IOException("File not found: " + filePath);
        }
        return Files.readAllBytes(fullPath);
    }
    
    public boolean deleteFile(String filePath) {
        try {
            Path fullPath = Paths.get(basePath, filePath);
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }
    
    public boolean fileExists(String filePath) {
        Path fullPath = Paths.get(basePath, filePath);
        return Files.exists(fullPath);
    }
    
    public long getFileSize(String filePath) throws IOException {
        Path fullPath = Paths.get(basePath, filePath);
        if (!Files.exists(fullPath)) {
            throw new IOException("File not found: " + filePath);
        }
        return Files.size(fullPath);
    }
    
    public String getContentType(String filePath) {
        String extension = getFileExtension(filePath);
        switch (extension.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt":
                return "text/plain";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default:
                return "application/octet-stream";
        }
    }
    
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        
        // Check file size (convert maxFileSize to bytes)
        long maxSizeBytes = parseFileSize(maxFileSize);
        if (file.getSize() > maxSizeBytes) {
            throw new IOException("File size exceeds maximum allowed size of " + maxFileSize);
        }
        
        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("File has no name");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS_LIST.contains(extension.toLowerCase())) {
            throw new IOException("File type not allowed. Allowed types: " + allowedExtensions);
        }
    }
    
    private Path createUserDirectory(Long userId) throws IOException {
        Path userDir = Paths.get(basePath, "user_" + userId);
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
        }
        return userDir;
    }
    
    private String generateUniqueFilename(String originalFilename, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + "." + extension;
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
    
    private long parseFileSize(String sizeString) {
        sizeString = sizeString.trim().toUpperCase();
        long multiplier = 1;
        
        if (sizeString.endsWith("KB")) {
            multiplier = 1024;
            sizeString = sizeString.substring(0, sizeString.length() - 2);
        } else if (sizeString.endsWith("MB")) {
            multiplier = 1024 * 1024;
            sizeString = sizeString.substring(0, sizeString.length() - 2);
        } else if (sizeString.endsWith("GB")) {
            multiplier = 1024 * 1024 * 1024;
            sizeString = sizeString.substring(0, sizeString.length() - 2);
        }
        
        try {
            return Long.parseLong(sizeString) * multiplier;
        } catch (NumberFormatException e) {
            return 10 * 1024 * 1024; // Default to 10MB
        }
    }
}