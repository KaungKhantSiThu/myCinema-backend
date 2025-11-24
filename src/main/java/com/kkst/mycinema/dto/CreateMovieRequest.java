package com.kkst.mycinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CreateMovieRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotNull(message = "Duration is required")
        @Positive(message = "Duration must be positive")
        Integer durationMinutes,

        @NotBlank(message = "Genre is required")
        String genre
) {}

