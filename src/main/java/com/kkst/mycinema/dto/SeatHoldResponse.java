package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "Response confirming seats are held")
public record SeatHoldResponse(
                @Schema(description = "Token to identify this hold session", example = "HOLD-12345") String holdToken,

                @Schema(description = "Show ID", example = "5") Long showId,

                @Schema(description = "Movie title", example = "Avatar 2") String movieTitle,

                @Schema(description = "Show start time", example = "2023-12-25T20:00:00") LocalDateTime showTime,

                @Schema(description = "List of held seats") List<SeatInfo> heldSeats,

                @Schema(description = "Time when the hold expires", example = "2023-12-25T19:55:00") LocalDateTime holdExpiresAt,

                @Schema(description = "Information message", example = "Seats held successfully for 10 minutes") String message) {
        @Builder
        @Schema(description = "Held seat details")
        public record SeatInfo(
                        @Schema(description = "Seat ID", example = "101") Long seatId,

                        @Schema(description = "Row number", example = "10") Integer rowNumber,

                        @Schema(description = "Seat number", example = "5") Integer seatNumber,

                        @Schema(description = "Price", example = "15.00") java.math.BigDecimal price) {
        }
}
