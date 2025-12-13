package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Layout of seats for a show")
public record ShowSeatsResponse(
                @Schema(description = "Show ID", example = "1") Long showId,

                @Schema(description = "Movie title", example = "Inception") String movieTitle,

                @Schema(description = "Map of row numbers to lists of seats") Map<Integer, List<SeatResponse>> seatsByRow) {
}
