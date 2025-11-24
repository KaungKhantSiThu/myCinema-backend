package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.LoginRequest;
import com.kkst.mycinema.dto.RegisterRequest;
import com.kkst.mycinema.entity.User;
import com.kkst.mycinema.repository.UserRepository;
import com.kkst.mycinema.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("$2a$10$hashedPassword")
                .roles("ROLE_USER")
                .build();
    }

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User user = i.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtUtil.generateToken(anyString())).thenReturn("jwt-token");

        // Act
        var response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals(registerRequest.email(), response.email());
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals(registerRequest.email()) &&
                user.getPassword().equals("$2a$10$hashedPassword") &&
                user.getRoles().equals("ROLE_USER")
        ));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));
        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(testUser.getEmail())).thenReturn("jwt-token");

        // Act
        var response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals(loginRequest.email(), response.email());
        assertEquals("Login successful", response.message());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    void register_NullEmail_ThrowsException() {
        // Arrange
        var invalidRequest = RegisterRequest.builder()
                .email(null)
                .password("password123")
                .build();

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> authService.register(invalidRequest));
    }

    @Test
    void register_EmptyPassword_ThrowsException() {
        // Arrange
        var invalidRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("")
                .build();

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> authService.register(invalidRequest));
    }
}

