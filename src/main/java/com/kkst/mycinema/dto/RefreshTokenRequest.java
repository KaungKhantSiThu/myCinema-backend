package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "Request to refresh access token")
public record RefreshTokenRequest(
                @NotBlank(message = "Refresh token is required") @Schema(description = "Valid refresh token", example = "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...") String refreshToken) {
}
