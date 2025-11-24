package com.kkst.mycinema.service;

import com.kkst.mycinema.entity.Movie;
import com.kkst.mycinema.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie testMovie1;
    private Movie testMovie2;

    @BeforeEach
    void setUp() {
        testMovie1 = Movie.builder()
                .id(1L)
                .title("Inception")
                .durationMinutes(148)
                .genre("Sci-Fi/Thriller")
                .build();

        testMovie2 = Movie.builder()
                .id(2L)
                .title("The Dark Knight")
                .durationMinutes(152)
                .genre("Action/Crime")
                .build();
    }

    @Test
    void getAllMovies_ReturnsAllMovies() {
        // Arrange
        List<Movie> movies = Arrays.asList(testMovie1, testMovie2);
        when(movieRepository.findAll()).thenReturn(movies);

        // Act
        var result = movieService.getAllMovies();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Inception", result.get(0).title());
        assertEquals("The Dark Knight", result.get(1).title());
        verify(movieRepository).findAll();
    }

    @Test
    void getAllMovies_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(movieRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        var result = movieService.getAllMovies();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllMovies_VerifiesCorrectMapping() {
        // Arrange
        when(movieRepository.findAll()).thenReturn(Collections.singletonList(testMovie1));

        // Act
        var result = movieService.getAllMovies();

        // Assert
        var movieResponse = result.get(0);
        assertEquals(testMovie1.getId(), movieResponse.id());
        assertEquals(testMovie1.getTitle(), movieResponse.title());
        assertEquals(testMovie1.getDurationMinutes(), movieResponse.durationMinutes());
        assertEquals(testMovie1.getGenre(), movieResponse.genre());
    }
}

