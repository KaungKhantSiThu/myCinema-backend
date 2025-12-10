package com.kkst.mycinema.external.tmdb;

import com.kkst.mycinema.external.ExternalMovieData;
import com.kkst.mycinema.external.MovieDataSource;
import com.kkst.mycinema.tmdbclient.TmdbClient;
import com.kkst.mycinema.tmdbclient.mapper.TmdbMovieMapper;
import com.kkst.mycinema.tmdbclient.model.TmdbMovieDetails;
import com.kkst.mycinema.tmdbclient.model.TmdbPagedResponse;
import com.kkst.mycinema.tmdbclient.model.TmdbMovie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * TMDb implementation of MovieDataSource using our custom TMDb client.
 * This adapter is only enabled when tmdb.api.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbMovieDataSource implements MovieDataSource {

    private final TmdbClient tmdbClient;
    private final TmdbMovieMapper mapper;

    public TmdbMovieDataSource(TmdbClient tmdbClient, TmdbMovieMapper mapper) {
        this.tmdbClient = tmdbClient;
        this.mapper = mapper;
        log.info("TMDb MovieDataSource initialized with custom client");
    }

    @Override
    public List<ExternalMovieData> searchMovies(String query, int page) {
        try {
            log.info("Searching TMDb for: '{}', page: {}", query, page);
            TmdbPagedResponse<TmdbMovie> response = tmdbClient.searchMovies(query, page);

            if (response == null || response.getResults() == null) {
                log.warn("TMDb search returned null results for query: '{}', page: {}", query, page);
                return Collections.emptyList();
            }

            int resultCount = response.getResults().size();
            log.info("TMDb search successful: query='{}', page={}, results={}, totalResults={}, totalPages={}",
                    query, page, resultCount, response.getTotalResults(), response.getTotalPages());

            List<ExternalMovieData> movies = mapper.toExternalMovieDataList(response.getResults());

            if (movies.isEmpty()) {
                log.info("No movies found in TMDb for query: '{}'", query);
            }

            return movies;
        } catch (Exception e) {
            log.error("Error searching TMDb for query: '{}', page: {}. Error: {}",
                    query, page, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<ExternalMovieData> getMovieById(String externalId) {
        try {
            int tmdbId = Integer.parseInt(externalId);
            log.info("Fetching TMDb movie details with ID: {}", tmdbId);

            Optional<TmdbMovieDetails> movieDetails = tmdbClient.getMovieDetails(tmdbId);

            if (movieDetails.isEmpty()) {
                log.warn("TMDb movie not found with ID: {}", tmdbId);
                return Optional.empty();
            }

            TmdbMovieDetails details = movieDetails.get();
            log.info("TMDb movie found: ID={}, title='{}', runtime={} min",
                    tmdbId, details.getTitle(), details.getRuntime());

            ExternalMovieData movieData = mapper.toExternalMovieData(details);
            return Optional.ofNullable(movieData);

        } catch (NumberFormatException e) {
            log.error("Invalid TMDb ID format: '{}'. Expected numeric value.", externalId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching TMDb movie with ID: '{}'. Error: {}",
                    externalId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "TMDb";
    }
}

