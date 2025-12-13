package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Show details")
public record ShowResponse(
                @Schema(description = "Show ID", example = "1") Long id,

                @Schema(description = "Movie ID", example = "10") Long movieId,

                @Schema(description = "Movie title", example = "The Godfather") String movieTitle,

                @Schema(description = "Hall name", example = "Hall A") String hallName,

                @Schema(description = "Start time", example = "2023-12-25T18:00:00") LocalDateTime startTime,

                @Schema(description = "End time", example = "2023-12-25T21:00:00") LocalDateTime endTime) {
}
