package com.kkst.mycinema.dto;

import lombok.Builder;

@Builder
public record MovieResponse(
        Long id,
        String title,
        Integer durationMinutes,
        String genre
) {}


