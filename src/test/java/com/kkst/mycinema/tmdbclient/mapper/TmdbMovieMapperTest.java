package com.kkst.mycinema.tmdbclient.mapper;

import com.kkst.mycinema.external.ExternalMovieData;
import com.kkst.mycinema.tmdbclient.model.TmdbGenre;
import com.kkst.mycinema.tmdbclient.model.TmdbMovie;
import com.kkst.mycinema.tmdbclient.model.TmdbMovieDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TmdbMovieMapper.
 */
class TmdbMovieMapperTest {

    private TmdbMovieMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TmdbMovieMapper();
    }

    @Test
    void testMapTmdbMovieToExternalMovieData() {
        // Given
        TmdbMovie movie = TmdbMovie.builder()
            .id(12345)
            .title("Test Movie")
            .originalTitle("Test Movie Original")
            .overview("A test movie overview")
            .releaseDate("2023-05-15")
            .posterPath("/poster.jpg")
            .backdropPath("/backdrop.jpg")
            .genreIds(Arrays.asList(28, 12, 878))
            .voteAverage(8.5)
            .voteCount(1000)
            .originalLanguage("en")
            .popularity(100.5)
            .build();

        // When
        ExternalMovieData result = mapper.toExternalMovieData(movie);

        // Then
        assertNotNull(result);
        assertEquals("12345", result.externalId());
        assertEquals("Test Movie", result.title());
        assertEquals("A test movie overview", result.overview());
        assertEquals(LocalDate.of(2023, 5, 15), result.releaseDate());
        assertNull(result.runtime()); // Not available in search results
        assertEquals(3, result.genres().size());
        assertEquals("/poster.jpg", result.posterPath());
        assertEquals("/backdrop.jpg", result.backdropPath());
        assertEquals(8.5, result.voteAverage());
        assertEquals(1000, result.voteCount());
        assertEquals("en", result.originalLanguage());
    }

    @Test
    void testMapTmdbMovieDetailsToExternalMovieData() {
        // Given
        TmdbMovieDetails details = TmdbMovieDetails.builder()
            .id(12345)
            .title("Test Movie Details")
            .overview("Detailed overview")
            .releaseDate("2023-06-20")
            .runtime(148)
            .posterPath("/poster.jpg")
            .backdropPath("/backdrop.jpg")
            .genres(Arrays.asList(
                TmdbGenre.builder().id(28).name("Action").build(),
                TmdbGenre.builder().id(12).name("Adventure").build()
            ))
            .voteAverage(7.8)
            .voteCount(500)
            .originalLanguage("en")
            .build();

        // When
        ExternalMovieData result = mapper.toExternalMovieData(details);

        // Then
        assertNotNull(result);
        assertEquals("12345", result.externalId());
        assertEquals("Test Movie Details", result.title());
        assertEquals("Detailed overview", result.overview());
        assertEquals(LocalDate.of(2023, 6, 20), result.releaseDate());
        assertEquals(148, result.runtime());
        assertEquals(2, result.genres().size());
        assertTrue(result.genres().contains("Action"));
        assertTrue(result.genres().contains("Adventure"));
        assertEquals("/poster.jpg", result.posterPath());
        assertEquals("/backdrop.jpg", result.backdropPath());
        assertEquals(7.8, result.voteAverage());
        assertEquals(500, result.voteCount());
        assertEquals("en", result.originalLanguage());
    }

    @Test
    void testMapNullMovie() {
        ExternalMovieData result = mapper.toExternalMovieData((TmdbMovie) null);
        assertNull(result);
    }

    @Test
    void testMapNullMovieDetails() {
        ExternalMovieData result = mapper.toExternalMovieData((TmdbMovieDetails) null);
        assertNull(result);
    }

    @Test
    void testMapMovieWithNullFields() {
        // Given - movie with minimal fields
        TmdbMovie movie = TmdbMovie.builder()
            .id(999)
            .title("Minimal Movie")
            .build();

        // When
        ExternalMovieData result = mapper.toExternalMovieData(movie);

        // Then
        assertNotNull(result);
        assertEquals("999", result.externalId());
        assertEquals("Minimal Movie", result.title());
        assertNull(result.overview());
        assertNull(result.releaseDate());
        assertNull(result.runtime());
        assertTrue(result.genres().isEmpty());
    }

    @Test
    void testMapMovieWithInvalidReleaseDate() {
        // Given
        TmdbMovie movie = TmdbMovie.builder()
            .id(123)
            .title("Invalid Date Movie")
            .releaseDate("invalid-date-format")
            .build();

        // When
        ExternalMovieData result = mapper.toExternalMovieData(movie);

        // Then
        assertNotNull(result);
        assertNull(result.releaseDate(), "Invalid date should result in null");
    }

    @Test
    void testMapMovieWithEmptyReleaseDate() {
        // Given
        TmdbMovie movie = TmdbMovie.builder()
            .id(123)
            .title("Empty Date Movie")
            .releaseDate("")
            .build();

        // When
        ExternalMovieData result = mapper.toExternalMovieData(movie);

        // Then
        assertNotNull(result);
        assertNull(result.releaseDate(), "Empty date should result in null");
    }

    @Test
    void testMapMovieList() {
        // Given
        List<TmdbMovie> movies = Arrays.asList(
            TmdbMovie.builder().id(1).title("Movie 1").build(),
            TmdbMovie.builder().id(2).title("Movie 2").build(),
            TmdbMovie.builder().id(3).title("Movie 3").build()
        );

        // When
        List<ExternalMovieData> results = mapper.toExternalMovieDataList(movies);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals("1", results.get(0).externalId());
        assertEquals("Movie 1", results.get(0).title());
        assertEquals("2", results.get(1).externalId());
        assertEquals("Movie 2", results.get(1).title());
        assertEquals("3", results.get(2).externalId());
        assertEquals("Movie 3", results.get(2).title());
    }

    @Test
    void testMapEmptyMovieList() {
        List<ExternalMovieData> results = mapper.toExternalMovieDataList(Collections.emptyList());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testMapNullMovieList() {
        List<ExternalMovieData> results = mapper.toExternalMovieDataList(null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testMapMovieWithNullGenreIds() {
        // Given
        TmdbMovie movie = TmdbMovie.builder()
            .id(100)
            .title("No Genres Movie")
            .genreIds(null)
            .build();

        // When
        ExternalMovieData result = mapper.toExternalMovieData(movie);

        // Then
        assertNotNull(result);
        assertNotNull(result.genres());
        assertTrue(result.genres().isEmpty());
    }

    @Test
    void testMapMovieDetailsWithNullGenres() {
        // Given
        TmdbMovieDetails details = TmdbMovieDetails.builder()
            .id(100)
            .title("No Genres Details")
            .genres(null)
            .build();

        // When
        ExternalMovieData result = mapper.toExternalMovieData(details);

        // Then
        assertNotNull(result);
        assertNotNull(result.genres());
        assertTrue(result.genres().isEmpty());
    }
}

