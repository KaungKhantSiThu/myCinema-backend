package com.kkst.mycinema.tmdbclient.example;

import com.kkst.mycinema.tmdbclient.TmdbClient;
import com.kkst.mycinema.tmdbclient.model.TmdbMovie;
import com.kkst.mycinema.tmdbclient.model.TmdbMovieDetails;
import com.kkst.mycinema.tmdbclient.model.TmdbPagedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Example usage of the custom TMDb client.
 * This class demonstrates how to use the TMDb client for various operations.
 *
 * NOTE: This is for demonstration purposes only.
 * In production, these operations would be called from services or controllers.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbClientUsageExample {

    private final TmdbClient tmdbClient;

    public TmdbClientUsageExample(TmdbClient tmdbClient) {
        this.tmdbClient = tmdbClient;
        log.info("TmdbClientUsageExample initialized");
    }

    /**
     * Example: Search for movies by title.
     */
    public void exampleSearchMovies() {
        log.info("=== Example: Search Movies ===");

        try {
            TmdbPagedResponse<TmdbMovie> results = tmdbClient.searchMovies("Inception", 1);

            log.info("Search Results:");
            log.info("- Total Results: {}", results.getTotalResults());
            log.info("- Total Pages: {}", results.getTotalPages());
            log.info("- Current Page: {}", results.getPage());

            if (results.getResults() != null && !results.getResults().isEmpty()) {
                TmdbMovie firstMovie = results.getResults().get(0);
                log.info("- First Result: {} (ID: {}, Release: {})",
                    firstMovie.getTitle(),
                    firstMovie.getId(),
                    firstMovie.getReleaseDate());
            }
        } catch (Exception e) {
            log.error("Error searching movies: {}", e.getMessage(), e);
        }
    }

    /**
     * Example: Get detailed movie information.
     */
    public void exampleGetMovieDetails() {
        log.info("=== Example: Get Movie Details ===");

        try {
            // Inception's TMDb ID is 27205
            Optional<TmdbMovieDetails> details = tmdbClient.getMovieDetails(27205);

            if (details.isPresent()) {
                TmdbMovieDetails movie = details.get();
                log.info("Movie Details:");
                log.info("- Title: {}", movie.getTitle());
                log.info("- Original Title: {}", movie.getOriginalTitle());
                log.info("- Tagline: {}", movie.getTagline());
                log.info("- Runtime: {} minutes", movie.getRuntime());
                log.info("- Release Date: {}", movie.getReleaseDate());
                log.info("- Vote Average: {}/10", movie.getVoteAverage());
                log.info("- Budget: ${}", movie.getBudget());
                log.info("- Revenue: ${}", movie.getRevenue());
                log.info("- Status: {}", movie.getStatus());

                if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
                    String genres = movie.getGenres().stream()
                        .map(g -> g.getName())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                    log.info("- Genres: {}", genres);
                }
            } else {
                log.warn("Movie not found");
            }
        } catch (Exception e) {
            log.error("Error getting movie details: {}", e.getMessage(), e);
        }
    }

    /**
     * Example: Get popular movies.
     */
    public void exampleGetPopularMovies() {
        log.info("=== Example: Get Popular Movies ===");

        try {
            TmdbPagedResponse<TmdbMovie> popular = tmdbClient.getPopularMovies(1);

            log.info("Popular Movies (Top 5):");
            popular.getResults().stream()
                .limit(5)
                .forEach(movie -> {
                    log.info("- {} ({})", movie.getTitle(), movie.getReleaseDate());
                });
        } catch (Exception e) {
            log.error("Error getting popular movies: {}", e.getMessage(), e);
        }
    }

    /**
     * Example: Get now playing movies.
     */
    public void exampleGetNowPlayingMovies() {
        log.info("=== Example: Get Now Playing Movies ===");

        try {
            TmdbPagedResponse<TmdbMovie> nowPlaying = tmdbClient.getNowPlayingMovies(1);

            log.info("Now Playing Movies (Top 5):");
            nowPlaying.getResults().stream()
                .limit(5)
                .forEach(movie -> {
                    log.info("- {} (Rating: {}/10)", movie.getTitle(), movie.getVoteAverage());
                });
        } catch (Exception e) {
            log.error("Error getting now playing movies: {}", e.getMessage(), e);
        }
    }

    /**
     * Example: Get upcoming movies.
     */
    public void exampleGetUpcomingMovies() {
        log.info("=== Example: Get Upcoming Movies ===");

        try {
            TmdbPagedResponse<TmdbMovie> upcoming = tmdbClient.getUpcomingMovies(1);

            log.info("Upcoming Movies (Top 5):");
            upcoming.getResults().stream()
                .limit(5)
                .forEach(movie -> {
                    log.info("- {} (Release: {})", movie.getTitle(), movie.getReleaseDate());
                });
        } catch (Exception e) {
            log.error("Error getting upcoming movies: {}", e.getMessage(), e);
        }
    }

    /**
     * Example: Get top rated movies.
     */
    public void exampleGetTopRatedMovies() {
        log.info("=== Example: Get Top Rated Movies ===");

        try {
            TmdbPagedResponse<TmdbMovie> topRated = tmdbClient.getTopRatedMovies(1);

            log.info("Top Rated Movies (Top 5):");
            topRated.getResults().stream()
                .limit(5)
                .forEach(movie -> {
                    log.info("- {} (Rating: {}/10, Votes: {})",
                        movie.getTitle(),
                        movie.getVoteAverage(),
                        movie.getVoteCount());
                });
        } catch (Exception e) {
            log.error("Error getting top rated movies: {}", e.getMessage(), e);
        }
    }

    /**
     * Example: Using the movie service directly for more control.
     */
    public void exampleUsingMovieService() {
        log.info("=== Example: Using Movie Service Directly ===");

        try {
            // Search with custom parameters
            TmdbPagedResponse<TmdbMovie> results = tmdbClient.movies()
                .searchMovies("Avatar", 1, "en-US", false);

            log.info("Search with custom parameters:");
            log.info("- Query: Avatar");
            log.info("- Language: en-US");
            log.info("- Include Adult: false");
            log.info("- Results: {}", results.getTotalResults());

            if (!results.getResults().isEmpty()) {
                TmdbMovie movie = results.getResults().get(0);
                log.info("- Top Result: {} ({})", movie.getTitle(), movie.getReleaseDate());
            }
        } catch (Exception e) {
            log.error("Error using movie service: {}", e.getMessage(), e);
        }
    }

    /**
     * Run all examples.
     * NOTE: This should only be called manually for testing, not in production.
     */
    public void runAllExamples() {
        log.info("=".repeat(60));
        log.info("Running TMDb Client Usage Examples");
        log.info("=".repeat(60));

        exampleSearchMovies();
        System.out.println();

        exampleGetMovieDetails();
        System.out.println();

        exampleGetPopularMovies();
        System.out.println();

        exampleGetNowPlayingMovies();
        System.out.println();

        exampleGetUpcomingMovies();
        System.out.println();

        exampleGetTopRatedMovies();
        System.out.println();

        exampleUsingMovieService();

        log.info("=".repeat(60));
        log.info("Examples completed");
        log.info("=".repeat(60));
    }
}

