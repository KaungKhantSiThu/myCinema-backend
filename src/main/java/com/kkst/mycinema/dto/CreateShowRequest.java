package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Request to create a new show")
public record CreateShowRequest(
                @NotNull(message = "Movie ID is required") @Schema(description = "ID of the movie", example = "10") Long movieId,

                @NotNull(message = "Hall ID is required") @Schema(description = "ID of the hall", example = "2") Long hallId,

                @NotNull(message = "Start time is required") @Future(message = "Start time must be in the future") @Schema(description = "Show start time", example = "2023-12-25T14:00:00") LocalDateTime startTime,

                @NotNull(message = "End time is required") @Schema(description = "Show end time", example = "2023-12-25T16:30:00") LocalDateTime endTime) {
}
