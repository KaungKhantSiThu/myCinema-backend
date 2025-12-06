package com.kkst.mycinema.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record SeatHoldResponse(
        String holdToken,
        Long showId,
        String movieTitle,
        LocalDateTime showTime,
        List<SeatInfo> heldSeats,
        LocalDateTime holdExpiresAt,
        String message
) {
    @Builder
    public record SeatInfo(
            Long seatId,
            Integer rowNumber,
            Integer seatNumber,
            java.math.BigDecimal price
    ) {}
}

