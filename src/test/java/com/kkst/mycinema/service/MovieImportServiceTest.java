package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.ExternalMovieSearchResponse;
import com.kkst.mycinema.dto.ImportMovieRequest;
import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.entity.Movie;
import com.kkst.mycinema.exception.MovieNotFoundException;
import com.kkst.mycinema.external.ExternalMovieData;
import com.kkst.mycinema.external.MovieDataSource;
import com.kkst.mycinema.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MovieImportService.
 * Tests the business logic for searching and importing movies from external sources.
 */
@ExtendWith(MockitoExtension.class)
class MovieImportServiceTest {

    @Mock
    private MovieDataSource movieDataSource;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieImportService movieImportService;

    private ExternalMovieData sampleExternalMovie;
    private Movie sampleMovie;

    @BeforeEach
    void setUp() {
        sampleExternalMovie = ExternalMovieData.builder()
                .externalId("550")
                .title("Fight Club")
                .overview("A ticking-time-bomb insomniac and a slippery soap salesman...")
                .releaseDate(LocalDate.of(1999, 10, 15))
                .runtime(139)
                .genres(List.of("Drama", "Thriller", "Comedy"))
                .posterPath("/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg")
                .backdropPath("/fCayJrkfRaCRCTh8GqN30f8oyQF.jpg")
                .voteAverage(8.4)
                .voteCount(26000)
                .originalLanguage("en")
                .build();

        sampleMovie = Movie.builder()
                .id(1L)
                .title("Fight Club")
                .durationMinutes(139)
                .genre("Drama")
                .build();
    }

    // ==================== Search Movies Tests ====================

    @Test
    void searchMovies_WithValidQuery_ReturnsResults() {
        // Arrange
        when(movieDataSource.searchMovies("fight club", 1))
                .thenReturn(List.of(sampleExternalMovie));
        when(movieDataSource.getSourceName()).thenReturn("TMDb");

        // Act
        List<ExternalMovieSearchResponse> results = movieImportService.searchMovies("fight club", 1);

        // Assert
        assertThat(results).hasSize(1);
        ExternalMovieSearchResponse result = results.get(0);
        assertThat(result.externalId()).isEqualTo("550");
        assertThat(result.title()).isEqualTo("Fight Club");
        assertThat(result.overview()).contains("insomniac");
        assertThat(result.runtime()).isEqualTo(139);
        assertThat(result.voteAverage()).isEqualTo(8.4);
        assertThat(result.source()).isEqualTo("TMDb");
        assertThat(result.genres()).containsExactly("Drama", "Thriller", "Comedy");

        verify(movieDataSource).searchMovies("fight club", 1);
        verify(movieDataSource, atLeastOnce()).getSourceName();
    }

    @Test
    void searchMovies_WithNoResults_ReturnsEmptyList() {
        // Arrange
        when(movieDataSource.searchMovies("nonexistent", 1))
                .thenReturn(Collections.emptyList());
        when(movieDataSource.getSourceName()).thenReturn("TMDb");

        // Act
        List<ExternalMovieSearchResponse> results = movieImportService.searchMovies("nonexistent", 1);

        // Assert
        assertThat(results).isEmpty();
        verify(movieDataSource).searchMovies("nonexistent", 1);
    }

    @Test
    void searchMovies_WithMultiplePages_ReturnsCorrectPage() {
        // Arrange
        when(movieDataSource.searchMovies("action", 2))
                .thenReturn(List.of(sampleExternalMovie));
        when(movieDataSource.getSourceName()).thenReturn("TMDb");

        // Act
        List<ExternalMovieSearchResponse> results = movieImportService.searchMovies("action", 2);

        // Assert
        assertThat(results).hasSize(1);
        verify(movieDataSource).searchMovies("action", 2);
    }

    // ==================== Import Movie Tests ====================

    @Test
    void importMovie_WithValidExternalId_SavesAndReturnsMovie() {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .genre("Drama")
                .build();

        when(movieDataSource.getMovieById("550"))
                .thenReturn(Optional.of(sampleExternalMovie));
        when(movieDataSource.getSourceName()).thenReturn("TMDb");
        when(movieRepository.save(any(Movie.class)))
                .thenReturn(sampleMovie);

        // Act
        MovieResponse response = movieImportService.importMovie(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Fight Club");
        assertThat(response.durationMinutes()).isEqualTo(139);
        assertThat(response.genre()).isEqualTo("Drama");

        // Verify movie was saved with correct data
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie savedMovie = movieCaptor.getValue();
        assertThat(savedMovie.getTitle()).isEqualTo("Fight Club");
        assertThat(savedMovie.getDurationMinutes()).isEqualTo(139);
        assertThat(savedMovie.getGenre()).isEqualTo("Drama");
    }

    @Test
    void importMovie_WithoutGenre_UsesFirstGenreFromExternalData() {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .build();  // No genre specified

        when(movieDataSource.getMovieById("550"))
                .thenReturn(Optional.of(sampleExternalMovie));
        when(movieDataSource.getSourceName()).thenReturn("TMDb");
        when(movieRepository.save(any(Movie.class)))
                .thenReturn(sampleMovie);

        // Act
        MovieResponse response = movieImportService.importMovie(request);

        // Assert
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie savedMovie = movieCaptor.getValue();
        assertThat(savedMovie.getGenre()).isEqualTo("Drama");  // First genre from external data
    }

    @Test
    void importMovie_WithNullRuntime_UsesDefaultDuration() {
        // Arrange
        ExternalMovieData movieWithoutRuntime = ExternalMovieData.builder()
                .externalId("550")
                .title("Fight Club")
                .overview("Overview")
                .runtime(null)  // No runtime
                .genres(List.of("Drama"))
                .build();

        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .genre("Drama")
                .build();

        when(movieDataSource.getMovieById("550"))
                .thenReturn(Optional.of(movieWithoutRuntime));
        when(movieDataSource.getSourceName()).thenReturn("TMDb");
        when(movieRepository.save(any(Movie.class)))
                .thenReturn(sampleMovie);

        // Act
        movieImportService.importMovie(request);

        // Assert
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie savedMovie = movieCaptor.getValue();
        assertThat(savedMovie.getDurationMinutes()).isEqualTo(120);  // Default value
    }

    @Test
    void importMovie_WithNoGenres_UsesUnknownGenre() {
        // Arrange
        ExternalMovieData movieWithoutGenres = ExternalMovieData.builder()
                .externalId("550")
                .title("Fight Club")
                .overview("Overview")
                .runtime(139)
                .genres(Collections.emptyList())  // No genres
                .build();

        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .build();  // No genre override

        when(movieDataSource.getMovieById("550"))
                .thenReturn(Optional.of(movieWithoutGenres));
        when(movieDataSource.getSourceName()).thenReturn("TMDb");
        when(movieRepository.save(any(Movie.class)))
                .thenReturn(sampleMovie);

        // Act
        movieImportService.importMovie(request);

        // Assert
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie savedMovie = movieCaptor.getValue();
        assertThat(savedMovie.getGenre()).isEqualTo("Unknown");
    }

    @Test
    void importMovie_WithInvalidExternalId_ThrowsMovieNotFoundException() {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("999999")
                .genre("Action")
                .build();

        when(movieDataSource.getMovieById("999999"))
                .thenReturn(Optional.empty());
        when(movieDataSource.getSourceName()).thenReturn("TMDb");

        // Act & Assert
        assertThatThrownBy(() -> movieImportService.importMovie(request))
                .isInstanceOf(MovieNotFoundException.class)
                .hasMessageContaining("Movie not found in TMDb")
                .hasMessageContaining("999999");

        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void importMovie_OverridesGenreWhenProvided() {
        // Arrange
        ImportMovieRequest request = ImportMovieRequest.builder()
                .externalId("550")
                .genre("Action")  // Override with Action instead of Drama
                .build();

        when(movieDataSource.getMovieById("550"))
                .thenReturn(Optional.of(sampleExternalMovie));
        when(movieDataSource.getSourceName()).thenReturn("TMDb");
        when(movieRepository.save(any(Movie.class)))
                .thenReturn(sampleMovie);

        // Act
        movieImportService.importMovie(request);

        // Assert
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie savedMovie = movieCaptor.getValue();
        assertThat(savedMovie.getGenre()).isEqualTo("Action");  // Overridden genre
    }
}

