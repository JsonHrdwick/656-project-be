package com.example.springbootjava.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private Environment environment;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Get allowed origins from properties
        String allowedOrigins = environment.getProperty("cors.allowed-origins", "http://localhost:3000");
        String[] origins = allowedOrigins.split(",");
        
        // Get allowed methods from properties
        String allowedMethods = environment.getProperty("cors.allowed-methods", "GET,POST,PUT,DELETE,OPTIONS");
        String[] methods = allowedMethods.split(",");
        
        // Get allowed headers from properties - use explicit headers when credentials are allowed
        String allowedHeaders = environment.getProperty("cors.allowed-headers", "Content-Type,Authorization,X-Requested-With,Accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers");
        String[] headers = allowedHeaders.split(",");
        
        // Get credentials setting from properties
        boolean allowCredentials = environment.getProperty("cors.allow-credentials", Boolean.class, true);
        
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods(methods)
                .allowedHeaders(headers)
                .allowCredentials(allowCredentials);
    }
}
