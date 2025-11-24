package com.kkst.mycinema.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record SeatResponse(
        Long seatId,
        Integer rowNumber,
        Integer seatNumber,
        String status,
        BigDecimal price
) {}

