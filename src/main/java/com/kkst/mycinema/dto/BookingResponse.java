package com.kkst.mycinema.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BookingResponse(
        Long bookingId,
        Long showId,
        String movieTitle,
        LocalDateTime showTime,
        List<SeatInfo> seats,
        BigDecimal totalAmount,
        LocalDateTime bookingTime,
        String status
) {
    @Builder
    public record SeatInfo(
            Integer rowNumber,
            Integer seatNumber,
            BigDecimal price
    ) {}
}

