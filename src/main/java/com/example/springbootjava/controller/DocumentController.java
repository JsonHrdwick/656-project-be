package com.example.springbootjava.controller;

import com.example.springbootjava.dto.DocumentResponseDTO;
import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.service.DocumentService;
import com.example.springbootjava.service.LocalFileStorageService;
import com.example.springbootjava.service.FileCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController extends BaseController {
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private LocalFileStorageService fileStorageService;
    
    @Autowired
    private FileCleanupService fileCleanupService;
    
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint(Authentication authentication) {
        try {
            System.out.println("=== TEST ENDPOINT DEBUG ===");
            System.out.println("Authentication: " + (authentication != null ? "Present" : "Null"));
            
            ResponseEntity<?> authCheck = checkAuthentication(authentication);
            if (authCheck != null) {
                System.out.println("Authentication check failed");
                return authCheck;
            }
            
            User user = getCurrentUser(authentication);
            System.out.println("User: " + user.getEmail());
            System.out.println("User ID: " + user.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Test endpoint working",
                "user", user.getEmail(),
                "userId", user.getId()
            ));
        } catch (Exception e) {
            System.out.println("=== TEST ENDPOINT ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                          Authentication authentication) {
        try {
            System.out.println("=== UPLOAD DEBUG START ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("File content type: " + file.getContentType());
            System.out.println("Authentication: " + (authentication != null ? "Present" : "Null"));
            
            ResponseEntity<?> authCheck = checkAuthentication(authentication);
            if (authCheck != null) {
                System.out.println("Authentication check failed");
                return authCheck;
            }
            
            User user = getCurrentUser(authentication);
            System.out.println("User: " + user.getEmail());
            
            Document document = documentService.uploadDocument(file, user);
            System.out.println("Document created with ID: " + document.getId());
            System.out.println("=== UPLOAD DEBUG END ===");
            
            // Convert to DTO to avoid lazy loading issues
            DocumentResponseDTO responseDTO = new DocumentResponseDTO(document);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            System.out.println("=== UPLOAD ERROR ===");
            System.out.println("Error type: " + e.getClass().getSimpleName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=== UPLOAD ERROR END ===");
            
            return ResponseEntity.badRequest()
                    .body("Error uploading file: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getUserDocuments(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Document> documents = documentService.getUserDocuments(user);
        List<DocumentResponseDTO> responseDTOs = documents.stream()
                .map(DocumentResponseDTO::new)
                .toList();
        return ResponseEntity.ok(responseDTOs);
    }
    
    @GetMapping("/page")
    public ResponseEntity<Page<Document>> getUserDocumentsPage(Pageable pageable,
                                                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<Document> documents = documentService.getUserDocuments(user, pageable);
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocument(@PathVariable Long id,
                                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Document> document = documentService.getDocumentById(id);
        
        if (document.isPresent() && document.get().getUser().getId().equals(user.getId())) {
            DocumentResponseDTO responseDTO = new DocumentResponseDTO(document.get());
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponseDTO>> searchDocuments(@RequestParam String q,
                                                        Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Document> documents = documentService.searchDocuments(user, q);
        List<DocumentResponseDTO> responseDTOs = documents.stream()
                .map(DocumentResponseDTO::new)
                .toList();
        return ResponseEntity.ok(responseDTOs);
    }
    
    @GetMapping("/type/{fileType}")
    public ResponseEntity<List<DocumentResponseDTO>> getDocumentsByType(@PathVariable String fileType,
                                                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Document> documents = documentService.getDocumentsByFileType(user, fileType);
        List<DocumentResponseDTO> responseDTOs = documents.stream()
                .map(DocumentResponseDTO::new)
                .toList();
        return ResponseEntity.ok(responseDTOs);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Object> getDocumentStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        long totalDocuments = documentService.getDocumentCount(user);
        long processedDocuments = documentService.getProcessedDocumentCount(user);
        
        return ResponseEntity.ok(Map.of(
                "totalDocuments", totalDocuments,
                "processedDocuments", processedDocuments,
                "pendingDocuments", totalDocuments - processedDocuments
        ));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id,
                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        documentService.deleteDocument(id, user);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id,
                                                   Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Optional<Document> documentOpt = documentService.getDocumentById(id);
            
            if (documentOpt.isEmpty() || !documentOpt.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            Document document = documentOpt.get();
            String filePath = document.getFilePath();
            
            // Check if file exists and is not a mock file
            if (filePath == null || filePath.startsWith("mock://") || !fileStorageService.fileExists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            // Retrieve file content
            byte[] fileContent = fileStorageService.retrieveFile(filePath);
            String contentType = fileStorageService.getContentType(filePath);
            
            // Create resource
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getTitle() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileContent.length));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (IOException e) {
            System.err.println("Error downloading file: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long id,
                                               Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Optional<Document> documentOpt = documentService.getDocumentById(id);
            
            if (documentOpt.isEmpty() || !documentOpt.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            Document document = documentOpt.get();
            String filePath = document.getFilePath();
            
            // Check if file exists and is not a mock file
            if (filePath == null || filePath.startsWith("mock://") || !fileStorageService.fileExists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            // Retrieve file content
            byte[] fileContent = fileStorageService.retrieveFile(filePath);
            String contentType = fileStorageService.getContentType(filePath);
            
            // Create resource
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            // Set headers for inline viewing
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getTitle() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileContent.length));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (IOException e) {
            System.err.println("Error viewing file: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/storage/stats")
    public ResponseEntity<Map<String, Object>> getStorageStats(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            // Get document stats
            long totalDocuments = documentService.getDocumentCount(user);
            long processedDocuments = documentService.getProcessedDocumentCount(user);
            
            // Get file storage stats
            FileCleanupService.StorageStats storageStats = fileCleanupService.getStorageStats();
            
            return ResponseEntity.ok(Map.of(
                "documents", Map.of(
                    "total", totalDocuments,
                    "processed", processedDocuments,
                    "pending", totalDocuments - processedDocuments
                ),
                "storage", Map.of(
                    "fileCount", storageStats.getFileCount(),
                    "directoryCount", storageStats.getDirectoryCount(),
                    "totalSizeBytes", storageStats.getTotalSizeBytes(),
                    "formattedSize", storageStats.getFormattedSize()
                )
            ));
            
        } catch (Exception e) {
            System.err.println("Error getting storage stats: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
