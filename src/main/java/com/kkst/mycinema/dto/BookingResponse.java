package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "Successful booking details")
public record BookingResponse(
                @Schema(description = "Unique booking ID", example = "1001") Long bookingId,

                @Schema(description = "Show ID", example = "50") Long showId,

                @Schema(description = "Movie title", example = "Inception") String movieTitle,

                @Schema(description = "Show start time", example = "2023-12-25T18:00:00") LocalDateTime showTime,

                @Schema(description = "List of booked seats") List<SeatInfo> seats,

                @Schema(description = "Total booking amount", example = "25.00") BigDecimal totalAmount,

                @Schema(description = "Time of booking", example = "2023-12-20T10:30:00") LocalDateTime bookingTime,

                @Schema(description = "Booking status", example = "CONFIRMED") String status) {
        @Builder
        @Schema(description = "Seat information")
        public record SeatInfo(
                        @Schema(description = "Row number", example = "5") Integer rowNumber,

                        @Schema(description = "Seat number", example = "12") Integer seatNumber,

                        @Schema(description = "Price for this seat", example = "12.50") BigDecimal price) {
        }
}
