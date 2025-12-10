package com.kkst.mycinema.external;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents external movie data from an external source (e.g., TMDb).
 * This DTO separates external movie data from internal Movie entity.
 */
@Builder
public record ExternalMovieData(
        String externalId,
        String title,
        String overview,
        LocalDate releaseDate,
        Integer runtime,
        List<String> genres,
        String posterPath,
        String backdropPath,
        Double voteAverage,
        Integer voteCount,
        String originalLanguage
) {
}
