package com.example.springbootjava.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Order(1)
public class AuthSecurityConfig {

    @Bean
    public SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/auth/**")
            .cors(cors -> cors.and()) // Enable CORS
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> 
                auth.anyRequest().permitAll()
            );
        
        return http.build();
    }
}
