package com.kkst.mycinema.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CreateShowRequest(
        @NotNull(message = "Movie ID is required")
        Long movieId,

        @NotNull(message = "Hall ID is required")
        Long hallId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        LocalDateTime endTime
) {}

