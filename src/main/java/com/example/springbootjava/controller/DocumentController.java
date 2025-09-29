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
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                          Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Document document = documentService.uploadDocument(file, user);
            return ResponseEntity.ok(document);
        } catch (IOException e) {
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
