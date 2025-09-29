package com.example.springbootjava.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupConfig {
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("==========================================");
        System.out.println("🚀 Application is ready and running!");
        System.out.println("==========================================");
    }
}
