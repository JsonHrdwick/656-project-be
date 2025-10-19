package com.example.springbootjava.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class StartupConfig {
    
    @Value("${document.storage.local.base-path:./uploads}")
    private String basePath;
    
    @Value("${document.storage.local.enabled:true}")
    private boolean localStorageEnabled;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("==========================================");
        System.out.println("🚀 Application is ready and running!");
        System.out.println("==========================================");
        
        // Initialize file storage directory
        if (localStorageEnabled) {
            initializeFileStorage();
        } else {
            System.out.println("Local file storage is disabled");
        }
    }
    
    private void initializeFileStorage() {
        try {
            Path uploadsDir = Paths.get(basePath);
            if (!Files.exists(uploadsDir)) {
                Files.createDirectories(uploadsDir);
                System.out.println("Created uploads directory: " + uploadsDir.toAbsolutePath());
            } else {
                System.out.println("Uploads directory already exists: " + uploadsDir.toAbsolutePath());
            }
            
            // Create .gitkeep file to ensure directory is tracked in git
            Path gitkeepFile = uploadsDir.resolve(".gitkeep");
            if (!Files.exists(gitkeepFile)) {
                Files.createFile(gitkeepFile);
                System.out.println("Created .gitkeep file in uploads directory");
            }
            
        } catch (IOException e) {
            System.err.println("Failed to initialize file storage directory: " + e.getMessage());
        }
    }
}
