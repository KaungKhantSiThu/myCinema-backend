package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.LoginRequest;
import com.kkst.mycinema.dto.RegisterRequest;
import com.kkst.mycinema.entity.User;
import com.kkst.mycinema.repository.UserRepository;
import com.kkst.mycinema.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.kkst.mycinema.dto.AuthResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        var user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // Generate token
        var token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
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

        // If authentication is successful, generate token
        var token = jwtUtil.generateToken(request.email());

        return AuthResponse.builder()
                .token(token)
                .email(request.email())
                .message("Login successful")
                .build();
    }
}


