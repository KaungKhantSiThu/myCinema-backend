package com.kkst.mycinema.exception;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record ErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp,
        String path,
        Map<String, String> errors
) {}


