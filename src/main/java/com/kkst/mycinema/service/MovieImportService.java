package com.kkst.mycinema.service;

import com.kkst.mycinema.config.CacheConfig;
import com.kkst.mycinema.dto.ExternalMovieSearchResponse;
import com.kkst.mycinema.dto.ImportMovieRequest;
import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.entity.Movie;
import com.kkst.mycinema.exception.MovieNotFoundException;
import com.kkst.mycinema.external.ExternalMovieData;
import com.kkst.mycinema.external.MovieDataSource;
import com.kkst.mycinema.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for importing movies from external data sources.
 * Uses the MovieDataSource abstraction to support multiple providers.
 * Only enabled when tmdb.api.enabled=true.
 */
@Service
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MovieImportService {

        private final MovieDataSource movieDataSource;
        private final MovieRepository movieRepository;

        /**
         * Search for movies from the external data source.
         *
         * @param query the search query
         * @param page  the page number (1-based)
         * @return list of external movie search results
         */
        public List<ExternalMovieSearchResponse> searchMovies(String query, int page) {
                log.info("Searching external movies with query: '{}', page: {}", query, page);

                List<ExternalMovieData> results = movieDataSource.searchMovies(query, page);

                log.info("Found {} movies from {}", results.size(), movieDataSource.getSourceName());

                return results.stream()
                                .map(this::mapToSearchResponse)
                                .toList();
        }

        /**
         * Get now playing movies from external source.
         * 
         * @param page page number
         * @return list of movies
         */
        public List<ExternalMovieSearchResponse> getNowPlayingMovies(int page) {
                log.info("Fetching now playing movies, page: {}", page);
                return movieDataSource.getNowPlaying(page).stream()
                                .map(this::mapToSearchResponse)
                                .toList();
        }

        /**
         * Get upcoming movies from external source.
         * 
         * @param page page number
         * @return list of movies
         */
        public List<ExternalMovieSearchResponse> getUpcomingMovies(int page) {
                log.info("Fetching upcoming movies, page: {}", page);
                return movieDataSource.getUpcoming(page).stream()
                                .map(this::mapToSearchResponse)
                                .toList();
        }

        /**
         * Import a movie from the external data source into the local database.
         *
         * @param request the import request containing external ID and optional genre
         *                override
         * @return the imported movie response
         */
        @Transactional
        @CacheEvict(value = CacheConfig.MOVIES_CACHE, allEntries = true)
        public MovieResponse importMovie(ImportMovieRequest request) {
                log.info("Importing movie with external ID: {}", request.externalId());

                ExternalMovieData externalData = movieDataSource.getMovieById(request.externalId())
                                .orElseThrow(() -> new MovieNotFoundException(
                                                "Movie not found in " + movieDataSource.getSourceName() +
                                                                " with ID: " + request.externalId()));

                // Select genre: use provided genre or first from external data
                String genre = request.genre() != null && !request.genre().isBlank()
                                ? request.genre()
                                : (externalData.genres() != null && !externalData.genres().isEmpty()
                                                ? externalData.genres().get(0)
                                                : "Unknown");

                Movie movie = Movie.builder()
                                .title(externalData.title())
                                .durationMinutes(externalData.runtime() != null ? externalData.runtime() : 120)
                                .genre(genre)
                                .externalSource(movieDataSource.getSourceName())
                                .externalId(externalData.externalId())
                                .build();

                movie = movieRepository.save(movie);

                log.info("Movie imported successfully: {} (ID: {}) from {} (external ID: {})",
                                movie.getTitle(), movie.getId(), movie.getExternalSource(), movie.getExternalId());

                return mapToMovieResponse(movie);
        }

        private ExternalMovieSearchResponse mapToSearchResponse(ExternalMovieData data) {
                return ExternalMovieSearchResponse.builder()
                                .externalId(data.externalId())
                                .title(data.title())
                                .overview(data.overview())
                                .releaseDate(data.releaseDate())
                                .runtime(data.runtime())
                                .genres(data.genres())
                                .posterPath(data.posterPath())
                                .voteAverage(data.voteAverage())
                                .source(movieDataSource.getSourceName())
                                .build();
        }

        private MovieResponse mapToMovieResponse(Movie movie) {
                return MovieResponse.builder()
                                .id(movie.getId())
                                .title(movie.getTitle())
                                .durationMinutes(movie.getDurationMinutes())
                                .genre(movie.getGenre())
                                .build();
        }
}
