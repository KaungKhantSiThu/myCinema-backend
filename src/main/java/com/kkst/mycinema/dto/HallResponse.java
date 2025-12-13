package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Details of a cinema hall")
public record HallResponse(
                @Schema(description = "Hall ID", example = "1") Long id,

                @Schema(description = "Hall name", example = "IMAX 1") String name,

                @Schema(description = "Total number of rows", example = "10") Integer totalRows,

                @Schema(description = "Total number of columns (seats per row)", example = "15") Integer totalColumns) {
}
