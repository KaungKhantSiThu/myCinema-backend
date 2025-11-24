package com.kkst.mycinema.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        String email,
        String message
) {}

