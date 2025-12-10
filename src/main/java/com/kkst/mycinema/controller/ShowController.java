package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.ShowResponse;
import com.kkst.mycinema.dto.ShowSeatsResponse;
import com.kkst.mycinema.service.ShowService;

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
    public ResponseEntity<List<ShowResponse>> getShows(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showService.getShows(movieId, date));
    }

    @GetMapping("/{showId}/seats")
    public ResponseEntity<ShowSeatsResponse> getShowSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(showService.getShowSeats(showId));
    }
}

