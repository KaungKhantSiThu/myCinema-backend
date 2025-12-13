package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "Seat details")
public record SeatResponse(
                @Schema(description = "Seat ID", example = "55") Long seatId,

                @Schema(description = "Row number", example = "5") Integer rowNumber,

                @Schema(description = "Seat number", example = "12") Integer seatNumber,

                @Schema(description = "Seat status (AVAILABLE, BOOKED, HELD)", example = "AVAILABLE") String status,

                @Schema(description = "Price", example = "12.50") BigDecimal price) {
}
