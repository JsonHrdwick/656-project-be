package com.example.springbootjava.controller;

import com.example.springbootjava.service.DatabaseBackupService;
import com.example.springbootjava.service.DatabaseRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);

    @Autowired
    private DatabaseBackupService backupService;

    @Autowired
    private DatabaseRecoveryService recoveryService;

    /**
     * Create a complete database backup
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createBackup() {
        try {
            logger.info("Creating database backup");
            String backupPath = backupService.createBackup();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Backup created successfully");
            response.put("backupPath", backupPath);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to create backup", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create backup: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Create a backup for a specific user
     */
    @PostMapping("/create/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @userService.isCurrentUser(#userId))")
    public ResponseEntity<Map<String, Object>> createUserBackup(@PathVariable Long userId) {
        try {
            logger.info("Creating user backup for user ID: {}", userId);
            String backupPath = backupService.createUserBackup(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User backup created successfully");
            response.put("backupPath", backupPath);
            response.put("userId", userId);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to create user backup for user ID: {}", userId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create user backup: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * List all available backup files
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listBackups() {
        try {
            logger.info("Listing backup files");
            List<DatabaseBackupService.BackupFileInfo> backups = backupService.listBackups();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("backups", backups);
            response.put("count", backups.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to list backups", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to list backups: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Download a backup file
     */
    @GetMapping("/download/{fileName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String fileName) {
        try {
            logger.info("Downloading backup file: {}", fileName);
            
            String backupPath = "backups/" + fileName;
            File backupFile = new File(backupPath);
            
            if (!backupFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(backupFile);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(backupFile.length())
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("Failed to download backup file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a backup file
     */
    @DeleteMapping("/delete/{fileName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteBackup(@PathVariable String fileName) {
        try {
            logger.info("Deleting backup file: {}", fileName);
            
            String backupPath = "backups/" + fileName;
            Path path = Paths.get(backupPath);
            
            if (!Files.exists(path)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Backup file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Files.delete(path);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Backup file deleted successfully");
            response.put("fileName", fileName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to delete backup file: {}", fileName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete backup file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Clean up old backup files
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldBackups(@RequestParam(defaultValue = "10") int keepCount) {
        try {
            logger.info("Cleaning up old backups, keeping: {}", keepCount);
            int deletedCount = backupService.cleanupOldBackups(keepCount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cleanup completed successfully");
            response.put("deletedCount", deletedCount);
            response.put("keepCount", keepCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old backups", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to cleanup old backups: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Validate a backup file
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> validateBackup(@RequestParam String backupPath) {
        try {
            logger.info("Validating backup file: {}", backupPath);
            DatabaseRecoveryService.ValidationResult result = recoveryService.validateBackup(backupPath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", result.isValid());
            response.put("backupTimestamp", result.getBackupTimestamp());
            response.put("backupVersion", result.getBackupVersion());
            response.put("errors", result.getErrors());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to validate backup file: {}", backupPath, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to validate backup file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recover database from backup file
     */
    @PostMapping("/recover")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> recoverFromBackup(@RequestParam String backupPath) {
        try {
            logger.info("Recovering database from backup: {}", backupPath);
            DatabaseRecoveryService.RecoveryResult result = recoveryService.recoverFromBackup(backupPath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Database recovery completed");
            response.put("backupTimestamp", result.getBackupTimestamp());
            response.put("backupVersion", result.getBackupVersion());
            response.put("recoveryTimestamp", result.getRecoveryTimestamp());
            response.put("usersRestored", result.getUsersRestored());
            response.put("documentsRestored", result.getDocumentsRestored());
            response.put("flashcardsRestored", result.getFlashcardsRestored());
            response.put("quizzesRestored", result.getQuizzesRestored());
            response.put("quizQuestionsRestored", result.getQuizQuestionsRestored());
            response.put("quizAnswersRestored", result.getQuizAnswersRestored());
            response.put("quizAttemptsRestored", result.getQuizAttemptsRestored());
            response.put("quizAttemptAnswersRestored", result.getQuizAttemptAnswersRestored());
            response.put("flashcardStudySessionsRestored", result.getFlashcardStudySessionsRestored());
            response.put("errors", result.getErrors());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to recover database from backup: {}", backupPath, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to recover database: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recover user data from backup file
     */
    @PostMapping("/recover/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @userService.isCurrentUser(#userId))")
    public ResponseEntity<Map<String, Object>> recoverUserFromBackup(
            @PathVariable Long userId, 
            @RequestParam String backupPath) {
        try {
            logger.info("Recovering user data from backup: {} for user ID: {}", backupPath, userId);
            DatabaseRecoveryService.RecoveryResult result = recoveryService.recoverUserFromBackup(backupPath, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User data recovery completed");
            response.put("userId", userId);
            response.put("backupTimestamp", result.getBackupTimestamp());
            response.put("backupVersion", result.getBackupVersion());
            response.put("recoveryTimestamp", result.getRecoveryTimestamp());
            response.put("usersRestored", result.getUsersRestored());
            response.put("documentsRestored", result.getDocumentsRestored());
            response.put("flashcardsRestored", result.getFlashcardsRestored());
            response.put("quizzesRestored", result.getQuizzesRestored());
            response.put("quizQuestionsRestored", result.getQuizQuestionsRestored());
            response.put("quizAnswersRestored", result.getQuizAnswersRestored());
            response.put("quizAttemptsRestored", result.getQuizAttemptsRestored());
            response.put("quizAttemptAnswersRestored", result.getQuizAttemptAnswersRestored());
            response.put("flashcardStudySessionsRestored", result.getFlashcardStudySessionsRestored());
            response.put("errors", result.getErrors());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to recover user data from backup: {} for user ID: {}", backupPath, userId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to recover user data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload and recover from backup file
     */
    @PostMapping("/upload-and-recover")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadAndRecover(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Uploading and recovering from backup file: {}", file.getOriginalFilename());
            
            // Save uploaded file temporarily
            String tempDir = "temp";
            Files.createDirectories(Paths.get(tempDir));
            String tempFilePath = tempDir + "/" + file.getOriginalFilename();
            file.transferTo(new File(tempFilePath));
            
            try {
                // Validate backup
                DatabaseRecoveryService.ValidationResult validation = recoveryService.validateBackup(tempFilePath);
                if (!validation.isValid()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Invalid backup file");
                    response.put("errors", validation.getErrors());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
                
                // Recover from backup
                DatabaseRecoveryService.RecoveryResult result = recoveryService.recoverFromBackup(tempFilePath);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Database recovery completed from uploaded file");
                response.put("fileName", file.getOriginalFilename());
                response.put("backupTimestamp", result.getBackupTimestamp());
                response.put("backupVersion", result.getBackupVersion());
                response.put("recoveryTimestamp", result.getRecoveryTimestamp());
                response.put("usersRestored", result.getUsersRestored());
                response.put("documentsRestored", result.getDocumentsRestored());
                response.put("flashcardsRestored", result.getFlashcardsRestored());
                response.put("quizzesRestored", result.getQuizzesRestored());
                response.put("quizQuestionsRestored", result.getQuizQuestionsRestored());
                response.put("quizAnswersRestored", result.getQuizAnswersRestored());
                response.put("quizAttemptsRestored", result.getQuizAttemptsRestored());
                response.put("quizAttemptAnswersRestored", result.getQuizAttemptAnswersRestored());
                response.put("flashcardStudySessionsRestored", result.getFlashcardStudySessionsRestored());
                response.put("errors", result.getErrors());
                
                return ResponseEntity.ok(response);
                
            } finally {
                // Clean up temporary file
                Files.deleteIfExists(Paths.get(tempFilePath));
            }
            
        } catch (Exception e) {
            logger.error("Failed to upload and recover from backup file", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to upload and recover from backup: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get backup statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getBackupStats() {
        try {
            logger.info("Getting backup statistics");
            List<DatabaseBackupService.BackupFileInfo> backups = backupService.listBackups();
            
            long totalSize = backups.stream().mapToLong(DatabaseBackupService.BackupFileInfo::getSize).sum();
            int backupCount = backups.size();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("backupCount", backupCount);
            response.put("totalSizeBytes", totalSize);
            response.put("totalSizeMB", totalSize / (1024.0 * 1024.0));
            response.put("oldestBackup", backups.isEmpty() ? null : backups.get(backups.size() - 1).getLastModified());
            response.put("newestBackup", backups.isEmpty() ? null : backups.get(0).getLastModified());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get backup statistics", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get backup statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
