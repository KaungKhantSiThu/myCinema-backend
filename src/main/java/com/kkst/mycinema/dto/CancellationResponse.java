package com.kkst.mycinema.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CancellationResponse(
        Long bookingId,
        String status,
        String message,
        LocalDateTime cancelledAt
) {}

