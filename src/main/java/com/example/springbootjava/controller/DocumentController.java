package com.example.springbootjava.controller;

import com.example.springbootjava.entity.Document;
import com.example.springbootjava.entity.User;
import com.example.springbootjava.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            
            return ResponseEntity.ok(document);
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
    public ResponseEntity<List<Document>> getUserDocuments(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Document> documents = documentService.getUserDocuments(user);
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/page")
    public ResponseEntity<Page<Document>> getUserDocumentsPage(Pageable pageable,
                                                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<Document> documents = documentService.getUserDocuments(user, pageable);
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable Long id,
                                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Document> document = documentService.getDocumentById(id);
        
        if (document.isPresent() && document.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.ok(document.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchDocuments(@RequestParam String q,
                                                        Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Document> documents = documentService.searchDocuments(user, q);
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/type/{fileType}")
    public ResponseEntity<List<Document>> getDocumentsByType(@PathVariable String fileType,
                                                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Document> documents = documentService.getDocumentsByFileType(user, fileType);
        return ResponseEntity.ok(documents);
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
}
