package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.CreateMovieRequest;
import com.kkst.mycinema.dto.CreateShowRequest;
import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.dto.ShowResponse;
import com.kkst.mycinema.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin", description = "Administrative APIs for managing movies, shows, and analytics (ADMIN role required)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    private final AdminService adminService;

    // =====================================================
    // Movie Management
    // =====================================================

    @PostMapping("/movies")
    @Operation(summary = "Create a new movie", description = "Creates a new movie in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Movie created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody CreateMovieRequest request) {
        var response = adminService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/movies/{id}")
    @Operation(summary = "Update a movie", description = "Updates an existing movie's details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movie updated successfully"),
            @ApiResponse(responseCode = "404", description = "Movie not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody CreateMovieRequest request) {
        var response = adminService.updateMovie(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/movies/{id}")
    @Operation(summary = "Delete a movie", description = "Deletes a movie if it has no associated shows")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movie deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Movie not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete - movie has associated shows"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    public ResponseEntity<Map<String, String>> deleteMovie(@PathVariable Long id) {
        adminService.deleteMovie(id);
        return ResponseEntity.ok(Map.of("message", "Movie deleted successfully"));
    }

    // =====================================================
    // Show Management
    // =====================================================

    @PostMapping("/shows")
    @Operation(summary = "Create a new show", description = "Schedules a movie showing in a hall at a specific time")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Show created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Hall is already booked for this time slot"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest request) {
        var response = adminService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/shows/{id}")
    @Operation(summary = "Delete a show", description = "Deletes a show if it has no existing bookings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Show deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Show not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete - show has existing bookings"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    public ResponseEntity<Map<String, String>> deleteShow(@PathVariable Long id) {
        adminService.deleteShow(id);
        return ResponseEntity.ok(Map.of("message", "Show deleted successfully"));
    }

    @PutMapping("/shows/{id}")
    @Operation(summary = "Update a show", description = "Updates a show if it has no confirmed bookings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Show updated successfully"),
            @ApiResponse(responseCode = "404", description = "Show not found"),
            @ApiResponse(responseCode = "409", description = "Cannot update - show has confirmed bookings"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    public ResponseEntity<ShowResponse> updateShow(
            @PathVariable Long id,
            @Valid @RequestBody CreateShowRequest request) {
        var response = adminService.updateShow(id, request);
        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Analytics
    // =====================================================

    @GetMapping("/analytics/revenue")
    @Operation(summary = "Get revenue report", description = "Returns total revenue and booking statistics")
    @ApiResponse(responseCode = "200", description = "Revenue report retrieved successfully")
    public ResponseEntity<Map<String, Object>> getRevenueReport() {
        var report = adminService.getRevenueReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/analytics/popular-movies")
    @Operation(summary = "Get popular movies", description = "Returns movies ranked by number of bookings")
    @ApiResponse(responseCode = "200", description = "Popular movies report retrieved successfully")
    public ResponseEntity<Map<String, Object>> getPopularMovies() {
        var report = adminService.getPopularMovies();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/analytics/occupancy/{showId}")
    @Operation(summary = "Get show occupancy", description = "Returns seat occupancy statistics for a specific show")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Occupancy report retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Show not found")
    })
    public ResponseEntity<Map<String, Object>> getShowOccupancy(@PathVariable Long showId) {
        var report = adminService.getShowOccupancy(showId);
        return ResponseEntity.ok(report);
    }
}
