package com.kkst.mycinema.tmdbclient;

import com.kkst.mycinema.tmdbclient.exception.TmdbApiException;
import com.kkst.mycinema.tmdbclient.model.TmdbMovie;
import com.kkst.mycinema.tmdbclient.model.TmdbMovieDetails;
import com.kkst.mycinema.tmdbclient.model.TmdbPagedResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TmdbClient.
 * These tests require a valid TMDb API key and internet connection.
 * Tests are disabled by default (via profile) to avoid unnecessary API calls.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class TmdbClientIntegrationTest {

    @Autowired(required = false)
    private TmdbClient tmdbClient;

    @Test
    void testTmdbClientAvailability() {
        if (tmdbClient == null) {
            log.info("TMDb client is disabled (tmdb.api.enabled=false in test profile)");
            return;
        }

        assertNotNull(tmdbClient, "TMDb client should be available when enabled");
        log.info("✅ TMDb client is properly configured and available");
    }

    @Test
    void testSearchMovies() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            // Search for a well-known movie
            TmdbPagedResponse<TmdbMovie> results = tmdbClient.searchMovies("Inception", 1);

            assertNotNull(results, "Search results should not be null");
            assertNotNull(results.getResults(), "Results list should not be null");
            assertTrue(results.getTotalResults() > 0, "Should find at least one result");
            assertTrue(results.getResults().size() > 0, "Results should contain movies");

            // Verify first result
            TmdbMovie firstMovie = results.getResults().get(0);
            assertNotNull(firstMovie.getId(), "Movie ID should not be null");
            assertNotNull(firstMovie.getTitle(), "Movie title should not be null");

            log.info("✅ Search test passed - Found {} movies for 'Inception'", results.getTotalResults());
            log.info("   First result: {} (ID: {})", firstMovie.getTitle(), firstMovie.getId());
        } catch (TmdbApiException e) {
            log.error("❌ TMDb API error: {}", e.getMessage());
            fail("TMDb API call failed: " + e.getMessage());
        }
    }

    @Test
    void testGetMovieDetails() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            // Inception's TMDb ID
            int inceptionId = 27205;
            Optional<TmdbMovieDetails> details = tmdbClient.getMovieDetails(inceptionId);

            assertTrue(details.isPresent(), "Movie details should be found");

            TmdbMovieDetails movie = details.get();
            assertEquals(inceptionId, movie.getId(), "Movie ID should match");
            assertNotNull(movie.getTitle(), "Title should not be null");
            assertNotNull(movie.getRuntime(), "Runtime should not be null");
            assertTrue(movie.getRuntime() > 0, "Runtime should be positive");

            log.info("✅ Movie details test passed");
            log.info("   Title: {}", movie.getTitle());
            log.info("   Runtime: {} minutes", movie.getRuntime());
            log.info("   Release Date: {}", movie.getReleaseDate());
            log.info("   Rating: {}/10", movie.getVoteAverage());
        } catch (TmdbApiException e) {
            log.error("❌ TMDb API error: {}", e.getMessage());
            fail("TMDb API call failed: " + e.getMessage());
        }
    }

    @Test
    void testGetPopularMovies() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            TmdbPagedResponse<TmdbMovie> popular = tmdbClient.getPopularMovies(1);

            assertNotNull(popular, "Popular movies response should not be null");
            assertNotNull(popular.getResults(), "Results should not be null");
            assertFalse(popular.getResults().isEmpty(), "Should have popular movies");

            log.info("✅ Popular movies test passed - Retrieved {} movies",
                popular.getResults().size());
        } catch (TmdbApiException e) {
            log.error("❌ TMDb API error: {}", e.getMessage());
            fail("TMDb API call failed: " + e.getMessage());
        }
    }

    @Test
    void testGetNowPlayingMovies() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            TmdbPagedResponse<TmdbMovie> nowPlaying = tmdbClient.getNowPlayingMovies(1);

            assertNotNull(nowPlaying, "Now playing response should not be null");
            assertNotNull(nowPlaying.getResults(), "Results should not be null");

            log.info("✅ Now playing movies test passed - Retrieved {} movies",
                nowPlaying.getResults().size());
        } catch (TmdbApiException e) {
            log.error("❌ TMDb API error: {}", e.getMessage());
            fail("TMDb API call failed: " + e.getMessage());
        }
    }

    @Test
    void testGetUpcomingMovies() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            TmdbPagedResponse<TmdbMovie> upcoming = tmdbClient.getUpcomingMovies(1);

            assertNotNull(upcoming, "Upcoming movies response should not be null");
            assertNotNull(upcoming.getResults(), "Results should not be null");

            log.info("✅ Upcoming movies test passed - Retrieved {} movies",
                upcoming.getResults().size());
        } catch (TmdbApiException e) {
            log.error("❌ TMDb API error: {}", e.getMessage());
            fail("TMDb API call failed: " + e.getMessage());
        }
    }

    @Test
    void testGetTopRatedMovies() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            TmdbPagedResponse<TmdbMovie> topRated = tmdbClient.getTopRatedMovies(1);

            assertNotNull(topRated, "Top rated response should not be null");
            assertNotNull(topRated.getResults(), "Results should not be null");
            assertFalse(topRated.getResults().isEmpty(), "Should have top rated movies");

            log.info("✅ Top rated movies test passed - Retrieved {} movies",
                topRated.getResults().size());
        } catch (TmdbApiException e) {
            log.error("❌ TMDb API error: {}", e.getMessage());
            fail("TMDb API call failed: " + e.getMessage());
        }
    }

    @Test
    void testPagination() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            TmdbPagedResponse<TmdbMovie> page1 = tmdbClient.searchMovies("Avatar", 1);
            TmdbPagedResponse<TmdbMovie> page2 = tmdbClient.searchMovies("Avatar", 2);

            assertNotNull(page1);
            assertNotNull(page2);
            assertEquals(1, page1.getPage());
            assertEquals(2, page2.getPage());

            // Results should be different
            if (!page1.getResults().isEmpty() && !page2.getResults().isEmpty()) {
                assertNotEquals(page1.getResults().get(0).getId(),
                              page2.getResults().get(0).getId(),
                              "Different pages should have different results");
            }

            log.info("✅ Pagination test passed");
            log.info("   Page 1 has {} results", page1.getResults().size());
            log.info("   Page 2 has {} results", page2.getResults().size());
        } catch (TmdbApiException e) {
            log.error("❌ TMDb API error: {}", e.getMessage());
            fail("TMDb API call failed: " + e.getMessage());
        }
    }

    @Test
    void testMovieNotFound() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            // Use an ID that's unlikely to exist
            Optional<TmdbMovieDetails> details = tmdbClient.getMovieDetails(999999999);

            assertFalse(details.isPresent(),
                "Non-existent movie should return empty Optional");

            log.info("✅ Movie not found test passed - Correctly handled missing movie");
        } catch (TmdbApiException e) {
            // This is also acceptable behavior
            log.info("✅ Movie not found test passed - Exception thrown for missing movie");
        }
    }

    @Test
    void testEmptySearchResults() {
        if (tmdbClient == null) {
            log.info("Skipping test - TMDb client is disabled");
            return;
        }

        try {
            // Search for something that probably doesn't exist
            TmdbPagedResponse<TmdbMovie> results =
                tmdbClient.searchMovies("xyzqwertyasdfzxcv123456789", 1);

            assertNotNull(results, "Response should not be null even for no results");
            assertNotNull(results.getResults(), "Results list should not be null");

            log.info("✅ Empty search test passed - Found {} results",
                results.getTotalResults());
        } catch (TmdbApiException e) {
            log.error("❌ TMDb API error: {}", e.getMessage());
            fail("TMDb API call failed: " + e.getMessage());
        }
    }
}

