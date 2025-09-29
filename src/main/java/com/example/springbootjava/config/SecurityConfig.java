package com.example.springbootjava.config;

import com.example.springbootjava.security.AuthEntryPointJwt;
import com.example.springbootjava.security.AuthTokenFilter;
import com.example.springbootjava.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;
    
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/health").permitAll()
                    .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Get allowed origins from properties with better error handling
        String allowedOrigins = environment.getProperty("cors.allowed-origins", "http://localhost:3000");
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            // Filter out empty strings and null values
            String[] origins = allowedOrigins.split(",");
            java.util.List<String> validOrigins = new java.util.ArrayList<>();
            for (String origin : origins) {
                if (origin != null && !origin.trim().isEmpty()) {
                    validOrigins.add(origin.trim());
                }
            }
            if (!validOrigins.isEmpty()) {
                configuration.setAllowedOrigins(validOrigins);
            } else {
                configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
            }
        } else {
            configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        }
        
        // Get allowed methods from properties
        String allowedMethods = environment.getProperty("cors.allowed-methods", "GET,POST,PUT,DELETE,OPTIONS");
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        
        // Get allowed headers from properties
        String allowedHeaders = environment.getProperty("cors.allowed-headers", "*");
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        
        // Get credentials setting from properties
        boolean allowCredentials = environment.getProperty("cors.allow-credentials", Boolean.class, true);
        configuration.setAllowCredentials(allowCredentials);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
