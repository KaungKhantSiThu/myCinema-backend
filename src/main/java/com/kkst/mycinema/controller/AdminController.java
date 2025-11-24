package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.CreateMovieRequest;
import com.kkst.mycinema.dto.CreateShowRequest;
import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.dto.ShowResponse;
import com.kkst.mycinema.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // Movie Management
    @PostMapping("/movies")
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody CreateMovieRequest request) {
        var response = adminService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/movies/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody CreateMovieRequest request) {
        var response = adminService.updateMovie(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<Map<String, String>> deleteMovie(@PathVariable Long id) {
        adminService.deleteMovie(id);
        return ResponseEntity.ok(Map.of("message", "Movie deleted successfully"));
    }

    // Show Management
    @PostMapping("/shows")
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest request) {
        var response = adminService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/shows/{id}")
    public ResponseEntity<Map<String, String>> deleteShow(@PathVariable Long id) {
        adminService.deleteShow(id);
        return ResponseEntity.ok(Map.of("message", "Show deleted successfully"));
    }

    // Analytics
    @GetMapping("/analytics/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueReport() {
        var report = adminService.getRevenueReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/analytics/popular-movies")
    public ResponseEntity<Map<String, Object>> getPopularMovies() {
        var report = adminService.getPopularMovies();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/analytics/occupancy/{showId}")
    public ResponseEntity<Map<String, Object>> getShowOccupancy(@PathVariable Long showId) {
        var report = adminService.getShowOccupancy(showId);
        return ResponseEntity.ok(report);
    }
}

