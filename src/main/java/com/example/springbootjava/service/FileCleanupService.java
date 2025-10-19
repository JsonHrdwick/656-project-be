package com.example.springbootjava.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Service
public class FileCleanupService {
    
    @Autowired
    private LocalFileStorageService fileStorageService;
    
    @Value("${document.storage.local.base-path:./uploads}")
    private String basePath;
    
    @Value("${file.cleanup.retention-days:30}")
    private int retentionDays;
    
    @Value("${file.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    /**
     * Clean up old files that are no longer referenced in the database
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrphanedFiles() {
        if (!cleanupEnabled) {
            System.out.println("File cleanup is disabled");
            return;
        }
        
        System.out.println("Starting file cleanup process...");
        
        try {
            Path uploadsDir = Paths.get(basePath);
            if (!Files.exists(uploadsDir)) {
                System.out.println("Uploads directory does not exist, skipping cleanup");
                return;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minus(retentionDays, ChronoUnit.DAYS);
            int deletedCount = 0;
            long totalSize = 0;
            
            // Walk through all files in the uploads directory
            try (Stream<Path> paths = Files.walk(uploadsDir)) {
                for (Path filePath : paths.filter(Files::isRegularFile).toList()) {
                    try {
                        // Check if file is older than retention period
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(filePath).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        
                        if (fileTime.isBefore(cutoffDate)) {
                            long fileSize = Files.size(filePath);
                            if (Files.deleteIfExists(filePath)) {
                                deletedCount++;
                                totalSize += fileSize;
                                System.out.println("Deleted old file: " + filePath);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error processing file " + filePath + ": " + e.getMessage());
                    }
                }
            }
            
            System.out.println("File cleanup completed. Deleted " + deletedCount + " files, freed " + 
                             formatFileSize(totalSize) + " of space");
            
        } catch (IOException e) {
            System.err.println("Error during file cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Clean up empty directories
     */
    @Scheduled(cron = "0 30 2 * * ?") // Run 30 minutes after file cleanup
    public void cleanupEmptyDirectories() {
        if (!cleanupEnabled) {
            return;
        }
        
        System.out.println("Starting empty directory cleanup...");
        
        try {
            Path uploadsDir = Paths.get(basePath);
            if (!Files.exists(uploadsDir)) {
                return;
            }
            
            int deletedCount = 0;
            
            // Walk through directories in reverse order (deepest first)
            try (Stream<Path> paths = Files.walk(uploadsDir)
                    .sorted((a, b) -> b.compareTo(a))) {
                for (Path dirPath : paths.filter(Files::isDirectory).toList()) {
                    // Skip the root uploads directory
                    if (dirPath.equals(uploadsDir)) {
                        continue;
                    }
                    
                    try {
                        // Check if directory is empty
                        if (Files.list(dirPath).findAny().isEmpty()) {
                            if (Files.deleteIfExists(dirPath)) {
                                deletedCount++;
                                System.out.println("Deleted empty directory: " + dirPath);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error processing directory " + dirPath + ": " + e.getMessage());
                    }
                }
            }
            
            System.out.println("Empty directory cleanup completed. Deleted " + deletedCount + " directories");
            
        } catch (IOException e) {
            System.err.println("Error during directory cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Get storage statistics
     */
    public StorageStats getStorageStats() {
        try {
            Path uploadsDir = Paths.get(basePath);
            if (!Files.exists(uploadsDir)) {
                return new StorageStats(0, 0, 0);
            }
            
            long totalSize = 0;
            int fileCount = 0;
            int dirCount = 0;
            
            try (Stream<Path> paths = Files.walk(uploadsDir)) {
                for (Path path : paths.toList()) {
                    if (Files.isRegularFile(path)) {
                        totalSize += Files.size(path);
                        fileCount++;
                    } else if (Files.isDirectory(path) && !path.equals(uploadsDir)) {
                        dirCount++;
                    }
                }
            }
            
            return new StorageStats(fileCount, dirCount, totalSize);
            
        } catch (IOException e) {
            System.err.println("Error getting storage stats: " + e.getMessage());
            return new StorageStats(0, 0, 0);
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    public static class StorageStats {
        private final int fileCount;
        private final int directoryCount;
        private final long totalSizeBytes;
        
        public StorageStats(int fileCount, int directoryCount, long totalSizeBytes) {
            this.fileCount = fileCount;
            this.directoryCount = directoryCount;
            this.totalSizeBytes = totalSizeBytes;
        }
        
        public int getFileCount() { return fileCount; }
        public int getDirectoryCount() { return directoryCount; }
        public long getTotalSizeBytes() { return totalSizeBytes; }
        
        public String getFormattedSize() {
            if (totalSizeBytes < 1024) return totalSizeBytes + " B";
            int exp = (int) (Math.log(totalSizeBytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "";
            return String.format("%.1f %sB", totalSizeBytes / Math.pow(1024, exp), pre);
        }
    }
}
