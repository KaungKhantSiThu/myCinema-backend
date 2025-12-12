package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "Request object for creating a new movie")
public record CreateMovieRequest(
                @NotBlank(message = "Title is required") @Schema(description = "Movie title", example = "The Godfather") String title,

                @Min(value = 1, message = "Duration must be at least 1 minute") @Schema(description = "Duration in minutes", example = "175") Integer durationMinutes,

                @NotBlank(message = "Genre is required") @Schema(description = "Movie genre", example = "Drama") String genre,

                @Schema(description = "Movie description") String description) {
}
