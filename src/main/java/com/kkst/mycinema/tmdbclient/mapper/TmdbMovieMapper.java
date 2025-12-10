package com.kkst.mycinema.tmdbclient.mapper;

import com.kkst.mycinema.external.ExternalMovieData;
import com.kkst.mycinema.tmdbclient.model.TmdbGenre;
import com.kkst.mycinema.tmdbclient.model.TmdbMovie;
import com.kkst.mycinema.tmdbclient.model.TmdbMovieDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps TMDb API models to application's ExternalMovieData.
 * Centralizes the conversion logic between TMDb and application domain models.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbMovieMapper {

    /**
     * Maps TMDb search movie result to ExternalMovieData.
     *
     * @param movie The TMDb movie from search results
     * @return ExternalMovieData instance
     */
    public ExternalMovieData toExternalMovieData(TmdbMovie movie) {
        if (movie == null) {
            return null;
        }

        LocalDate releaseDate = parseReleaseDate(movie.getReleaseDate());

        // For search results, genre IDs are returned as integers
        List<String> genres = movie.getGenreIds() != null
            ? movie.getGenreIds().stream()
                .map(String::valueOf)
                .collect(Collectors.toList())
            : Collections.emptyList();

        return ExternalMovieData.builder()
            .externalId(String.valueOf(movie.getId()))
            .title(movie.getTitle())
            .overview(movie.getOverview())
            .releaseDate(releaseDate)
            .runtime(null)  // Not available in search results
            .genres(genres)
            .posterPath(movie.getPosterPath())
            .backdropPath(movie.getBackdropPath())
            .voteAverage(movie.getVoteAverage())
            .voteCount(movie.getVoteCount())
            .originalLanguage(movie.getOriginalLanguage())
            .build();
    }

    /**
     * Maps TMDb movie details to ExternalMovieData.
     *
     * @param details The TMDb movie details
     * @return ExternalMovieData instance
     */
    public ExternalMovieData toExternalMovieData(TmdbMovieDetails details) {
        if (details == null) {
            return null;
        }

        LocalDate releaseDate = parseReleaseDate(details.getReleaseDate());

        // For movie details, full genre objects are returned
        List<String> genres = details.getGenres() != null
            ? details.getGenres().stream()
                .map(TmdbGenre::getName)
                .collect(Collectors.toList())
            : Collections.emptyList();

        return ExternalMovieData.builder()
            .externalId(String.valueOf(details.getId()))
            .title(details.getTitle())
            .overview(details.getOverview())
            .releaseDate(releaseDate)
            .runtime(details.getRuntime())
            .genres(genres)
            .posterPath(details.getPosterPath())
            .backdropPath(details.getBackdropPath())
            .voteAverage(details.getVoteAverage())
            .voteCount(details.getVoteCount())
            .originalLanguage(details.getOriginalLanguage())
            .build();
    }

    /**
     * Maps a list of TMDb movies to ExternalMovieData list.
     *
     * @param movies List of TMDb movies
     * @return List of ExternalMovieData
     */
    public List<ExternalMovieData> toExternalMovieDataList(List<TmdbMovie> movies) {
        if (movies == null) {
            return Collections.emptyList();
        }

        return movies.stream()
            .map(this::toExternalMovieData)
            .collect(Collectors.toList());
    }

    /**
     * Parses a release date string to LocalDate.
     * TMDb returns dates in format: "YYYY-MM-DD"
     *
     * @param releaseDateStr The release date string
     * @return LocalDate or null if parsing fails
     */
    private LocalDate parseReleaseDate(String releaseDateStr) {
        if (releaseDateStr == null || releaseDateStr.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(releaseDateStr);
        } catch (Exception e) {
            log.warn("Failed to parse release date: {}", releaseDateStr, e);
            return null;
        }
    }
}

