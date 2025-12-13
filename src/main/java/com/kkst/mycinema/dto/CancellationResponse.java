package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Response for booking cancellation")
public record CancellationResponse(
                @Schema(description = "ID of the cancelled booking", example = "1001") Long bookingId,

                @Schema(description = "Status of the cancellation", example = "CANCELLED") String status,

                @Schema(description = "Cancellation message", example = "Booking cancelled successfully") String message,

                @Schema(description = "Timestamp of cancellation", example = "2023-12-21T09:00:00") LocalDateTime cancelledAt) {
}
