package com.kkst.mycinema.dto;

import lombok.Builder;

@Builder
public record HallResponse(
        Long id,
        String name,
        Integer totalRows,
        Integer totalColumns) {
}
