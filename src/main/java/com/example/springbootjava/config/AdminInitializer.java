package com.example.springbootjava.config;

import com.example.springbootjava.entity.User;
import com.example.springbootjava.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;

@Configuration
public class AdminInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    @Value("${admin.default.email:admin@example.com}")
    private String defaultAdminEmail;

    @Value("${admin.default.password:Admin!234}")
    private String defaultAdminPassword;

    @Value("${admin.default.first-name:Admin}")
    private String defaultAdminFirstName;

    @Value("${admin.default.last-name:User}")
    private String defaultAdminLastName;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void ensureAdminUser() {
        try {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount > 0) {
                return;
            }

            userRepository.findByEmail(defaultAdminEmail).ifPresentOrElse(existing -> {
                if (existing.getRole() != User.Role.ADMIN) {
                    existing.setRole(User.Role.ADMIN);
                    existing.setEnabled(true);
                    userRepository.save(existing);
                    logger.info("Elevated existing user '{}' to ADMIN role", defaultAdminEmail);
                }
            }, () -> {
                User admin = new User();
                admin.setFirstName(defaultAdminFirstName);
                admin.setLastName(defaultAdminLastName);
                admin.setEmail(defaultAdminEmail);
                admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                admin.setRole(User.Role.ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);
                logger.info("Created default ADMIN user '{}'", defaultAdminEmail);
            });
        } catch (Exception e) {
            logger.error("Failed to ensure default admin user exists", e);
        }
    }
}


