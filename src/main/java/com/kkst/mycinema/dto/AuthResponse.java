package com.kkst.mycinema.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication response containing tokens and user info")
public record AuthResponse(
        @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...") String token,

        @Schema(description = "JWT Refresh Token", example = "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...") String refreshToken,

        @Schema(description = "User email address", example = "user@example.com") String email,

        @Schema(description = "User full name", example = "John Doe") String name,

        @Schema(description = "User role", example = "ROLE_USER") String role,

        @Schema(description = "Additional message", example = "Login successful") String message) {
}
