package com.kkst.mycinema.tmdbclient;

import com.kkst.mycinema.tmdbclient.model.TmdbMovie;
import com.kkst.mycinema.tmdbclient.model.TmdbMovieDetails;
import com.kkst.mycinema.tmdbclient.model.TmdbPagedResponse;
import com.kkst.mycinema.tmdbclient.service.TmdbMovieService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Main client facade for TMDb API.
 * Provides a unified interface for all TMDb operations.
 * This is the primary entry point for external code using the TMDb client.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbClient {

    private final TmdbMovieService movieService;

    public TmdbClient(TmdbMovieService movieService) {
        this.movieService = movieService;
        log.info("TMDb Client initialized and ready for use");
    }

    /**
     * Get the movie service for movie-related operations.
     *
     * @return TmdbMovieService instance
     */
    public TmdbMovieService movies() {
        return movieService;
    }

    // Convenience methods for common operations

    /**
     * Search for movies by query string.
     *
     * @param query The search query
     * @param page  The page number (1-based)
     * @return Paged response containing matching movies
     */
    public TmdbPagedResponse<TmdbMovie> searchMovies(String query, int page) {
        return movieService.searchMovies(query, page);
    }

    /**
     * Get detailed information about a movie by its TMDb ID.
     *
     * @param movieId The TMDb movie ID
     * @return Movie details if found
     */
    public Optional<TmdbMovieDetails> getMovieDetails(int movieId) {
        return movieService.getMovieDetails(movieId);
    }

    /**
     * Get popular movies.
     *
     * @param page The page number (1-based)
     * @return Paged response containing popular movies
     */
    public TmdbPagedResponse<TmdbMovie> getPopularMovies(int page) {
        return movieService.getPopularMovies(page);
    }

    /**
     * Get now playing movies.
     *
     * @param page The page number (1-based)
     * @return Paged response containing now playing movies
     */
    public TmdbPagedResponse<TmdbMovie> getNowPlayingMovies(int page) {
        return movieService.getNowPlayingMovies(page);
    }

    /**
     * Get upcoming movies.
     *
     * @param page The page number (1-based)
     * @return Paged response containing upcoming movies
     */
    public TmdbPagedResponse<TmdbMovie> getUpcomingMovies(int page) {
        return movieService.getUpcomingMovies(page);
    }

    /**
     * Get top rated movies.
     *
     * @param page The page number (1-based)
     * @return Paged response containing top rated movies
     */
    public TmdbPagedResponse<TmdbMovie> getTopRatedMovies(int page) {
        return movieService.getTopRatedMovies(page);
    }
}

