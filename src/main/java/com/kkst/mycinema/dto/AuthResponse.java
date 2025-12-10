package com.kkst.mycinema.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
                String token,
                String refreshToken,
                String email,
                String name,
                String role,
                String message) {
}
