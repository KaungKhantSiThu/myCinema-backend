package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.AuthResponse;
import com.kkst.mycinema.dto.LoginRequest;
import com.kkst.mycinema.dto.RefreshTokenRequest;
import com.kkst.mycinema.dto.RegisterRequest;
import com.kkst.mycinema.entity.User;
import com.kkst.mycinema.exception.EmailAlreadyExistsException;
import com.kkst.mycinema.exception.InvalidTokenException;
import com.kkst.mycinema.repository.UserRepository;
import com.kkst.mycinema.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        // Determine role - default to ROLE_USER if not specified
        String userRole = "ROLE_USER";
        if (request.role() != null && !request.role().isBlank()) {
            // Normalize role (add ROLE_ prefix if not present, convert to uppercase)
            String normalizedRole = request.role().toUpperCase().trim();
            if (!normalizedRole.startsWith("ROLE_")) {
                normalizedRole = "ROLE_" + normalizedRole;
            }
            // Only allow USER and ADMIN roles
            if (normalizedRole.equals("ROLE_ADMIN") || normalizedRole.equals("ROLE_USER")) {
                userRole = normalizedRole;
            }
        }

        // Create new user
        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(userRole)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // Generate tokens
        var accessToken = jwtUtil.generateToken(user.getEmail());
        var refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("User registered successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .message("User registered successfully")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // If authentication is successful, generate tokens
        var accessToken = jwtUtil.generateToken(request.email());
        var refreshToken = jwtUtil.generateRefreshToken(request.email());

        log.info("User logged in successfully: {}", request.email());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(request.email())
                .message("Login successful")
                .build();
    }

    /**
     * Refresh access token using a valid refresh token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        // Extract username from refresh token
        String username;
        try {
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            log.warn("Failed to extract username from refresh token");
            throw new InvalidTokenException("Invalid refresh token");
        }

        // Validate refresh token
        if (!jwtUtil.validateRefreshToken(refreshToken, username)) {
            log.warn("Refresh token validation failed for user: {}", username);
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        // Verify user exists
        if (!userRepository.existsByEmail(username)) {
            log.warn("User not found for refresh token: {}", username);
            throw new InvalidTokenException("User not found");
        }

        // Generate new access token
        var newAccessToken = jwtUtil.generateToken(username);

        log.info("Access token refreshed for user: {}", username);

        return AuthResponse.builder()
                .token(newAccessToken)
                .email(username)
                .message("Token refreshed successfully")
                .build();
    }
}


