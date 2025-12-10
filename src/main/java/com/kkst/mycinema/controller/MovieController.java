package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Tag(name = "Movies", description = "Movie listing and search APIs")
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    @Operation(summary = "Get all movies", description = "Returns a list of all available movies")
    @ApiResponse(responseCode = "200", description = "Movies retrieved successfully")
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie details", description = "Returns details of a specific movie")
    @ApiResponse(responseCode = "200", description = "Movie retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Movie not found")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get movies (paginated)", description = "Returns paginated list of movies with sorting options")
    @ApiResponse(responseCode = "200", description = "Movies page retrieved successfully")
    public ResponseEntity<Page<MovieResponse>> getMoviesPaginated(
            @Parameter(description = "Search query (title)") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by genre") @RequestParam(required = false) String genre,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field (title, genre, durationMinutes)") @RequestParam(defaultValue = "title") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        // Validate and sanitize inputs
        if (page < 0)
            page = 0;
        if (size < 1)
            size = 10;
        if (size > 50)
            size = 50;

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        var pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(movieService.getMoviesPaginated(query, genre, pageable));
    }
}
