package com.kkst.mycinema.tmdbclient.service;

import com.kkst.mycinema.tmdbclient.config.TmdbClientConfig;
import com.kkst.mycinema.tmdbclient.http.TmdbHttpClient;
import com.kkst.mycinema.tmdbclient.model.TmdbMovie;
import com.kkst.mycinema.tmdbclient.model.TmdbMovieDetails;
import com.kkst.mycinema.tmdbclient.model.TmdbPagedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for interacting with TMDb Movie API endpoints.
 * Provides methods for searching movies and retrieving movie details.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbMovieService {

    private final TmdbHttpClient httpClient;
    private final TmdbClientConfig config;

    public TmdbMovieService(TmdbHttpClient httpClient, TmdbClientConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    /**
     * Search for movies by query string.
     *
     * @param query The search query
     * @param page  The page number (1-based)
     * @return Paged response containing matching movies
     */
    public TmdbPagedResponse<TmdbMovie> searchMovies(String query, int page) {
        return searchMovies(query, page, null, null);
    }

    /**
     * Search for movies by query string with additional filters.
     *
     * @param query        The search query
     * @param page         The page number (1-based)
     * @param language     The language code (e.g., "en-US")
     * @param includeAdult Whether to include adult content
     * @return Paged response containing matching movies
     */
    public TmdbPagedResponse<TmdbMovie> searchMovies(String query, int page, String language, Boolean includeAdult) {
        log.info("Searching movies: query='{}', page={}, language={}, includeAdult={}",
            query, page, language, includeAdult);

        Map<String, String> params = new HashMap<>();
        params.put("query", query);
        params.put("page", String.valueOf(page));
        params.put("language", language != null ? language : config.getApi().getLanguage());
        params.put("include_adult", String.valueOf(
            includeAdult != null ? includeAdult : config.getApi().isIncludeAdult()
        ));

        TmdbPagedResponse<TmdbMovie> response = httpClient.get(
            "/search/movie",
            params,
            new ParameterizedTypeReference<TmdbPagedResponse<TmdbMovie>>() {}
        );

        log.info("Search successful: found {} results, page {} of {}",
            response.getTotalResults(), response.getPage(), response.getTotalPages());

        return response;
    }

    /**
     * Get detailed information about a movie by its TMDb ID.
     *
     * @param movieId The TMDb movie ID
     * @return Movie details if found
     */
    public Optional<TmdbMovieDetails> getMovieDetails(int movieId) {
        return getMovieDetails(movieId, null);
    }

    /**
     * Get detailed information about a movie by its TMDb ID with specific language.
     *
     * @param movieId  The TMDb movie ID
     * @param language The language code (e.g., "en-US")
     * @return Movie details if found
     */
    public Optional<TmdbMovieDetails> getMovieDetails(int movieId, String language) {
        log.info("Fetching movie details: movieId={}, language={}", movieId, language);

        Map<String, String> params = new HashMap<>();
        params.put("language", language != null ? language : config.getApi().getLanguage());

        try {
            TmdbMovieDetails details = httpClient.get(
                "/movie/" + movieId,
                params,
                TmdbMovieDetails.class
            );

            log.info("Movie details retrieved: id={}, title='{}', runtime={} min",
                details.getId(), details.getTitle(), details.getRuntime());

            return Optional.ofNullable(details);
        } catch (Exception e) {
            log.error("Failed to fetch movie details for ID {}: {}", movieId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get popular movies.
     *
     * @param page The page number (1-based)
     * @return Paged response containing popular movies
     */
    public TmdbPagedResponse<TmdbMovie> getPopularMovies(int page) {
        return getPopularMovies(page, null);
    }

    /**
     * Get popular movies with specific language.
     *
     * @param page     The page number (1-based)
     * @param language The language code (e.g., "en-US")
     * @return Paged response containing popular movies
     */
    public TmdbPagedResponse<TmdbMovie> getPopularMovies(int page, String language) {
        log.info("Fetching popular movies: page={}, language={}", page, language);

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("language", language != null ? language : config.getApi().getLanguage());

        TmdbPagedResponse<TmdbMovie> response = httpClient.get(
            "/movie/popular",
            params,
            new ParameterizedTypeReference<TmdbPagedResponse<TmdbMovie>>() {}
        );

        log.info("Popular movies retrieved: {} results, page {} of {}",
            response.getTotalResults(), response.getPage(), response.getTotalPages());

        return response;
    }

    /**
     * Get now playing movies.
     *
     * @param page The page number (1-based)
     * @return Paged response containing now playing movies
     */
    public TmdbPagedResponse<TmdbMovie> getNowPlayingMovies(int page) {
        return getNowPlayingMovies(page, null);
    }

    /**
     * Get now playing movies with specific language.
     *
     * @param page     The page number (1-based)
     * @param language The language code (e.g., "en-US")
     * @return Paged response containing now playing movies
     */
    public TmdbPagedResponse<TmdbMovie> getNowPlayingMovies(int page, String language) {
        log.info("Fetching now playing movies: page={}, language={}", page, language);

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("language", language != null ? language : config.getApi().getLanguage());

        TmdbPagedResponse<TmdbMovie> response = httpClient.get(
            "/movie/now_playing",
            params,
            new ParameterizedTypeReference<TmdbPagedResponse<TmdbMovie>>() {}
        );

        log.info("Now playing movies retrieved: {} results, page {} of {}",
            response.getTotalResults(), response.getPage(), response.getTotalPages());

        return response;
    }

    /**
     * Get upcoming movies.
     *
     * @param page The page number (1-based)
     * @return Paged response containing upcoming movies
     */
    public TmdbPagedResponse<TmdbMovie> getUpcomingMovies(int page) {
        return getUpcomingMovies(page, null);
    }

    /**
     * Get upcoming movies with specific language.
     *
     * @param page     The page number (1-based)
     * @param language The language code (e.g., "en-US")
     * @return Paged response containing upcoming movies
     */
    public TmdbPagedResponse<TmdbMovie> getUpcomingMovies(int page, String language) {
        log.info("Fetching upcoming movies: page={}, language={}", page, language);

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("language", language != null ? language : config.getApi().getLanguage());

        TmdbPagedResponse<TmdbMovie> response = httpClient.get(
            "/movie/upcoming",
            params,
            new ParameterizedTypeReference<TmdbPagedResponse<TmdbMovie>>() {}
        );

        log.info("Upcoming movies retrieved: {} results, page {} of {}",
            response.getTotalResults(), response.getPage(), response.getTotalPages());

        return response;
    }

    /**
     * Get top rated movies.
     *
     * @param page The page number (1-based)
     * @return Paged response containing top rated movies
     */
    public TmdbPagedResponse<TmdbMovie> getTopRatedMovies(int page) {
        return getTopRatedMovies(page, null);
    }

    /**
     * Get top rated movies with specific language.
     *
     * @param page     The page number (1-based)
     * @param language The language code (e.g., "en-US")
     * @return Paged response containing top rated movies
     */
    public TmdbPagedResponse<TmdbMovie> getTopRatedMovies(int page, String language) {
        log.info("Fetching top rated movies: page={}, language={}", page, language);

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("language", language != null ? language : config.getApi().getLanguage());

        TmdbPagedResponse<TmdbMovie> response = httpClient.get(
            "/movie/top_rated",
            params,
            new ParameterizedTypeReference<TmdbPagedResponse<TmdbMovie>>() {}
        );

        log.info("Top rated movies retrieved: {} results, page {} of {}",
            response.getTotalResults(), response.getPage(), response.getTotalPages());

        return response;
    }
}

