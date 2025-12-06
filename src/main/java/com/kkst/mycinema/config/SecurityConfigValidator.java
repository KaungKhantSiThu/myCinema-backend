package com.kkst.mycinema.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration validator that ensures critical security settings are properly configured.
 * Only runs in production profile.
 */
@Configuration
@Profile("prod")
@Slf4j
public class SecurityConfigValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private static final String DEFAULT_JWT_SECRET = "dev-only-secret-key-change-in-production-min-256-bits";
    private static final String DEFAULT_DB_PASSWORD = "cinema_pass";

    @PostConstruct
    public void validateSecurityConfiguration() {
        log.info("Validating production security configuration...");

        StringBuilder errors = new StringBuilder();

        // Validate JWT secret
        if (jwtSecret == null || jwtSecret.isBlank()) {
            errors.append("JWT_SECRET environment variable is not set. ");
        } else if (jwtSecret.equals(DEFAULT_JWT_SECRET)) {
            errors.append("JWT_SECRET is using default development value. Please set a secure secret. ");
        } else if (jwtSecret.length() < 32) {
            errors.append("JWT_SECRET is too short. Must be at least 32 characters for HS256. ");
        }

        // Validate database password
        if (dbPassword == null || dbPassword.isBlank()) {
            errors.append("DATABASE_PASSWORD environment variable is not set. ");
        } else if (dbPassword.equals(DEFAULT_DB_PASSWORD)) {
            errors.append("DATABASE_PASSWORD is using default development value. Please set a secure password. ");
        }

        if (!errors.isEmpty()) {
            log.error("SECURITY CONFIGURATION ERRORS: {}", errors);
            throw new SecurityException(
                    "Production security configuration is invalid: " + errors +
                    "Please set the required environment variables before running in production."
            );
        }

        log.info("Production security configuration validated successfully.");
    }
}

