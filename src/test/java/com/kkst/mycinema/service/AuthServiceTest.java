package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.LoginRequest;
import com.kkst.mycinema.dto.RefreshTokenRequest;
import com.kkst.mycinema.dto.RegisterRequest;
import com.kkst.mycinema.entity.User;
import com.kkst.mycinema.exception.EmailAlreadyExistsException;
import com.kkst.mycinema.exception.InvalidTokenException;
import com.kkst.mycinema.repository.UserRepository;
import com.kkst.mycinema.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .name("New User")
                .email("newuser@example.com")
                .password("Password1!")
                .build();

        loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("Password1!")
                .build();

        testUser = User.builder()
                .id(1L)
                .name("Test User")
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
        when(jwtUtil.generateToken(anyString())).thenReturn("jwt-access-token");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("jwt-refresh-token");

        // Act
        var response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-access-token", response.token());
        assertEquals("jwt-refresh-token", response.refreshToken());
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
        var exception = assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(registerRequest));
        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        // Arrange
        when(jwtUtil.generateToken(loginRequest.email())).thenReturn("jwt-access-token");
        when(jwtUtil.generateRefreshToken(loginRequest.email())).thenReturn("jwt-refresh-token");

        // Act
        var response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-access-token", response.token());
        assertEquals("jwt-refresh-token", response.refreshToken());
        assertEquals(loginRequest.email(), response.email());
        assertEquals("Login successful", response.message());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_InvalidCredentials_ThrowsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    void refreshToken_Success() {
        // Arrange
        var refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(jwtUtil.extractUsername("valid-refresh-token")).thenReturn("user@example.com");
        when(jwtUtil.validateRefreshToken("valid-refresh-token", "user@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        when(jwtUtil.generateToken("user@example.com")).thenReturn("new-access-token");

        // Act
        var response = authService.refreshToken(refreshRequest);

        // Assert
        assertNotNull(response);
        assertEquals("new-access-token", response.token());
        assertEquals("user@example.com", response.email());
        assertEquals("Token refreshed successfully", response.message());
    }

    @Test
    void refreshToken_InvalidToken_ThrowsException() {
        // Arrange
        var refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("invalid-refresh-token")
                .build();

        when(jwtUtil.extractUsername("invalid-refresh-token")).thenReturn("user@example.com");
        when(jwtUtil.validateRefreshToken("invalid-refresh-token", "user@example.com")).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(refreshRequest));
    }

    @Test
    void refreshToken_UserNotFound_ThrowsException() {
        // Arrange
        var refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(jwtUtil.extractUsername("valid-refresh-token")).thenReturn("nonexistent@example.com");
        when(jwtUtil.validateRefreshToken("valid-refresh-token", "nonexistent@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(refreshRequest));
    }
}

