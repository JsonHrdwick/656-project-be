package com.example.springbootjava.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "AI Study Platform Backend");
        
        // Check database connectivity
        try (Connection connection = dataSource.getConnection()) {
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("databaseError", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
    
    // Simple health check for Railway (lightweight)
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> simpleHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Service is running",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    // Railway-specific health check endpoint
    @GetMapping("/railway")
    public ResponseEntity<Map<String, String>> railwayHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "springboot-java",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    // Simple public health check (no authentication required)
    @GetMapping("/public/health")
    public ResponseEntity<Map<String, String>> publicHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AI Study Platform Backend",
                "timestamp", LocalDateTime.now().toString(),
                "message", "Public health check successful"
        ));
    }
}
