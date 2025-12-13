package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Movie details")
public record MovieResponse(
        @Schema(description = "Movie ID", example = "1") Long id,

        @Schema(description = "Movie title", example = "The Matrix") String title,

        @Schema(description = "Duration in minutes", example = "136") Integer durationMinutes,

        @Schema(description = "Genre", example = "Sci-Fi") String genre,

        @Schema(description = "Description", example = "A computer hacker learns from mysterious rebels about the true nature of his reality...") String description) {
}
