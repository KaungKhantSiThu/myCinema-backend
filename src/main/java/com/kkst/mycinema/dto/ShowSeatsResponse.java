package com.kkst.mycinema.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ShowSeatsResponse(
        Long showId,
        String movieTitle,
        Map<Integer, List<SeatResponse>> seatsByRow
) {}

