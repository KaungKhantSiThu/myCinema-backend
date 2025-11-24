package com.kkst.mycinema.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record BookingRequest(
        @NotNull(message = "Show ID is required")
        Long showId,

        @NotEmpty(message = "At least one seat must be selected")
        List<Long> seatIds
) {}

