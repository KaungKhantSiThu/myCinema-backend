package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Request DTO for importing a movie from an external source.
 */
@Builder
@Schema(description = "Request to import a movie from TMDb into the cinema database")
public record ImportMovieRequest(
        @NotBlank(message = "External ID is required")
        @Schema(
            description = "TMDb movie ID (obtained from search results)",
            example = "27205",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        String externalId,

        @Schema(
            description = "Optional: Override the genre for the movie. If not provided, the first genre from TMDb will be used.",
            example = "Action",
            nullable = true
        )
        String genre
) {
}
