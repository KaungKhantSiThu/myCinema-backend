package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Request to book seats for a show")
public record BookingRequest(
                @NotNull(message = "Show ID is required") @Schema(description = "ID of the show to book", example = "1") Long showId,

                @NotEmpty(message = "At least one seat must be selected") @Schema(description = "List of seat IDs to book", example = "[1, 2, 3]") List<Long> seatIds) {
}
