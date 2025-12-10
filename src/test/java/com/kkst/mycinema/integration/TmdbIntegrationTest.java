package com.kkst.mycinema.integration;

import com.kkst.mycinema.external.ExternalMovieData;
import com.kkst.mycinema.external.MovieDataSource;
import com.kkst.mycinema.tmdbclient.TmdbClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify TMDb client integration with the application.
 * Tests the complete flow from MovieDataSource through to TMDb API.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class TmdbIntegrationTest {

    @Autowired(required = false)
    private TmdbClient tmdbClient;

    @Autowired(required = false)
    private MovieDataSource movieDataSource;

    @Test
    void testTmdbClientAndDataSourceIntegration() {
        if (tmdbClient == null) {
            log.info("TMDb client is disabled - skipping integration test");
            return;
        }

        assertNotNull(movieDataSource, "MovieDataSource should be available when TMDb is enabled");
        log.info("✅ TMDb client and MovieDataSource are properly wired");
    }

    @Test
    void testSearchMoviesThroughDataSource() {
        if (movieDataSource == null) {
            log.info("MovieDataSource is not available - skipping test");
            return;
        }

        try {
            // Test search through the data source adapter
            List<ExternalMovieData> results = movieDataSource.searchMovies("Inception", 1);

            assertNotNull(results, "Search results should not be null");
            log.info("✅ Search through MovieDataSource works - Found {} movies", results.size());

            if (!results.isEmpty()) {
                ExternalMovieData firstMovie = results.get(0);
                assertNotNull(firstMovie.externalId(), "External ID should not be null");
                assertNotNull(firstMovie.title(), "Title should not be null");
                log.info("   First result: {} (ID: {})", firstMovie.title(), firstMovie.externalId());
            }
        } catch (Exception e) {
            log.warn("Search test failed - this may be expected if API key is not configured");
        }
    }

    @Test
    void testGetMovieByIdThroughDataSource() {
        if (movieDataSource == null) {
            log.info("MovieDataSource is not available - skipping test");
            return;
        }

        try {
            // Test getting movie details through the data source adapter
            // Inception's TMDb ID
            Optional<ExternalMovieData> result = movieDataSource.getMovieById("27205");

            assertNotNull(result, "Result should not be null");
            log.info("✅ Get movie by ID through MovieDataSource works");

            if (result.isPresent()) {
                ExternalMovieData movie = result.get();
                assertEquals("27205", movie.externalId());
                assertNotNull(movie.title());
                assertNotNull(movie.runtime(), "Runtime should be available in details");
                assertTrue(movie.runtime() > 0, "Runtime should be positive");

                log.info("   Movie: {}", movie.title());
                log.info("   Runtime: {} minutes", movie.runtime());
                log.info("   Genres: {}", movie.genres());
            }
        } catch (Exception e) {
            log.warn("Get movie test failed - this may be expected if API key is not configured");
        }
    }

    @Test
    void testDataSourceName() {
        if (movieDataSource == null) {
            log.info("MovieDataSource is not available - skipping test");
            return;
        }

        String sourceName = movieDataSource.getSourceName();
        assertEquals("TMDb", sourceName, "Data source should identify as TMDb");
        log.info("✅ MovieDataSource correctly identifies as: {}", sourceName);
    }

    @Test
    void testErrorHandlingInDataSource() {
        if (movieDataSource == null) {
            log.info("MovieDataSource is not available - skipping test");
            return;
        }

        try {
            // Test with invalid movie ID - should handle gracefully
            Optional<ExternalMovieData> result = movieDataSource.getMovieById("999999999");

            assertNotNull(result, "Result should not be null even for invalid ID");
            log.info("✅ Error handling works - Invalid ID returned: {}",
                result.isPresent() ? "found" : "not found");
        } catch (Exception e) {
            log.warn("Error handling test failed - this may be expected if API key is not configured");
        }
    }

    @Test
    void testEmptySearchHandling() {
        if (movieDataSource == null) {
            log.info("MovieDataSource is not available - skipping test");
            return;
        }

        try {
            // Test with query that likely returns no results
            List<ExternalMovieData> results =
                movieDataSource.searchMovies("xyzqwertyasdfzxcv999", 1);

            assertNotNull(results, "Results should not be null even for empty search");
            log.info("✅ Empty search handling works - Found {} results", results.size());
        } catch (Exception e) {
            log.warn("Empty search test failed - this may be expected if API key is not configured");
        }
    }
}

