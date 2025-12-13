package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.ShowResponse;
import com.kkst.mycinema.dto.ShowSeatsResponse;
import com.kkst.mycinema.service.ShowService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Tag(name = "Shows", description = "Showtime and seating APIs")
public class ShowController {

    private final ShowService showService;

    @GetMapping
    @Operation(summary = "Get shows", description = "Get available shows, optionally filtered by movie ID and date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shows retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameteters")
    })
    public ResponseEntity<List<ShowResponse>> getShows(
            @Parameter(description = "Movie ID to filter by", example = "1") @RequestParam(required = false) Long movieId,

            @Parameter(description = "Date to filter by (ISO format YYYY-MM-DD)", example = "2023-12-25") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showService.getShows(movieId, date));
    }

    @GetMapping("/{showId}/seats")
    @Operation(summary = "Get show seats", description = "Get seat layout and status for a specific show")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seat layout retrieved successfully", content = @Content(schema = @Schema(implementation = ShowSeatsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Show not found")
    })
    public ResponseEntity<ShowSeatsResponse> getShowSeats(
            @Parameter(description = "Show ID", example = "10") @PathVariable Long showId) {
        return ResponseEntity.ok(showService.getShowSeats(showId));
    }
}
