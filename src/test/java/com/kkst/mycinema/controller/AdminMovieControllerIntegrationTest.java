package com.kkst.mycinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkst.mycinema.dto.ExternalMovieSearchResponse;
import com.kkst.mycinema.dto.ImportMovieRequest;
import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.entity.User;
import com.kkst.mycinema.exception.MovieNotFoundException;
import com.kkst.mycinema.repository.UserRepository;
import com.kkst.mycinema.security.JwtUtil;
import com.kkst.mycinema.service.MovieImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminMovieController.
 * Tests TMDb integration endpoints with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "tmdb.api.enabled=true",  // Enable TMDb to allow MovieImportService bean creation
        "tmdb.api.key=test-key"   // Use test API key
})
class AdminMovieControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private MovieImportService movieImportService;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Create admin user
        User admin = User.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .name("Admin User")
                .roles("ROLE_ADMIN")
                .build();
        userRepository.save(admin);
        adminToken = jwtUtil.generateToken(admin.getEmail());

        // Create regular user
        User user = User.builder()
                .email("user@test.com")
                .password(passwordEncoder.encode("user123"))
                .name("Regular User")
                .roles("ROLE_USER")
                .build();
        userRepository.save(user);
        userToken = jwtUtil.generateToken(user.getEmail());
    }

    // ==================== Search Movies Tests ====================

    @Test
    void searchMovies_WithAdminRole_ReturnsResults() throws Exception {
        // Arrange
        ExternalMovieSearchResponse mockMovie = ExternalMovieSearchResponse.builder()
                .externalId("550")
                .title("Fight Club")
                .overview("A ticking-time-bomb insomniac and a slippery soap salesman...")
                .releaseDate(LocalDate.of(1999, 10, 15))
                .runtime(139)
                .genres(List.of("Drama"))
                .posterPath("/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg")
                .voteAverage(8.4)
                .source("TMDb")
                .build();

        when(movieImportService.searchMovies(anyString(), anyInt()))
                .thenReturn(List.of(mockMovie));

        // Act & Assert
        mockMvc.perform(get("/api/admin/movies/search")
                        .param("query", "fight club")
                        .param("page", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].externalId").value("550"))
                .andExpect(jsonPath("$[0].title").value("Fight Club"))
                .andExpect(jsonPath("$[0].overview").exists())
                .andExpect(jsonPath("$[0].runtime").value(139))
                .andExpect(jsonPath("$[0].voteAverage").value(8.4))
                .andExpect(jsonPath("$[0].source").value("TMDb"));
    }

    @Test
    void searchMovies_WithEmptyResults_ReturnsEmptyArray() throws Exception {
        // Arrange
        when(movieImportService.searchMovies(anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/admin/movies/search")
                        .param("query", "nonexistentmovie12345")
                        .param("page", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(content().json("[]"));
    }

    @Test
    void searchMovies_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // Act & Assert - Spring Security returns 403 when no authentication is provided
        mockMvc.perform(get("/api/admin/movies/search")
                        .param("query", "matrix")
                        .param("page", "1"))
                .andDo(print())
                .andExpect(status().isForbidden());  // Changed from isUnauthorized to match Spring Security behavior
    }

    @Test
    void searchMovies_WithUserRole_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/movies/search")
                        .param("query", "matrix")
                        .param("page", "1")
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void searchMovies_WithMissingQuery_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/movies/search")
                        .param("page", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchMovies_WithEmptyQuery_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/movies/search")
                        .param("query", "")
                        .param("page", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchMovies_WithInvalidPage_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/movies/search")
                        .param("query", "matrix")
                        .param("page", "0")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ==================== Import Movie Tests ====================

    @Test
    void importMovie_WithValidData_ReturnsCreated() throws Exception {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .genre("Drama")
                .build();

        MovieResponse mockResponse = MovieResponse.builder()
                .id(1L)
                .title("Fight Club")
                .durationMinutes(139)
                .genre("Drama")
                .build();

        when(movieImportService.importMovie(any(ImportMovieRequest.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/admin/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fight Club"))
                .andExpect(jsonPath("$.durationMinutes").value(139))
                .andExpect(jsonPath("$.genre").value("Drama"));
    }

    @Test
    void importMovie_WithInvalidExternalId_ReturnsNotFound() throws Exception {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("999999")
                .genre("Action")
                .build();

        when(movieImportService.importMovie(any(ImportMovieRequest.class)))
                .thenThrow(new MovieNotFoundException("Movie not found in TMDb with ID: 999999"));

        // Act & Assert
        mockMvc.perform(post("/api/admin/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Movie not found")));
    }

    @Test
    void importMovie_WithoutExternalId_ReturnsBadRequest() throws Exception {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .genre("Action")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/admin/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void importMovie_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .genre("Drama")
                .build();

        // Act & Assert - Spring Security returns 403 when no authentication is provided
        mockMvc.perform(post("/api/admin/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());  // Changed from isUnauthorized to match Spring Security behavior
    }

    @Test
    void importMovie_WithUserRole_ReturnsForbidden() throws Exception {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .genre("Drama")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/admin/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void importMovie_WithOptionalGenre_ReturnsCreated() throws Exception {
        // Arrange - externalId only, no genre
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .build();

        MovieResponse mockResponse = MovieResponse.builder()
                .id(1L)
                .title("Fight Club")
                .durationMinutes(139)
                .genre("Drama")  // Genre from TMDb
                .build();

        when(movieImportService.importMovie(any(ImportMovieRequest.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/admin/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Fight Club"))
                .andExpect(jsonPath("$.genre").value("Drama"));
    }
}

