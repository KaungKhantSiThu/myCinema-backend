package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for external movie search results.
 */
@Builder
@Schema(description = "Movie search result from TMDb")
public record ExternalMovieSearchResponse(
        @Schema(description = "TMDb movie ID (use this for importing)", example = "27205")
        String externalId,
        
        @Schema(description = "Movie title", example = "Inception")
        String title,
        
        @Schema(description = "Movie plot summary", example = "A thief who steals corporate secrets through dream-sharing technology...")
        String overview,
        
        @Schema(description = "Release date", example = "2010-07-16")
        LocalDate releaseDate,
        
        @Schema(description = "Runtime in minutes", example = "148")
        Integer runtime,
        
        @Schema(description = "List of genres", example = "[\"Action\", \"Science Fiction\", \"Thriller\"]")
        List<String> genres,
        
        @Schema(description = "TMDb poster path (relative URL)", example = "/qmDpIHrmpJINaRKAfWQfftjCdyi.jpg")
        String posterPath,
        
        @Schema(description = "TMDb average rating (0-10)", example = "8.3")
        Double voteAverage,
        
        @Schema(description = "Data source name", example = "TMDb")
        String source
) {
}
