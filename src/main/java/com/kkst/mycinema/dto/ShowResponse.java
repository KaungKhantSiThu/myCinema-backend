package com.kkst.mycinema.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ShowResponse(
        Long id,
        Long movieId,
        String movieTitle,
        String hallName,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}

