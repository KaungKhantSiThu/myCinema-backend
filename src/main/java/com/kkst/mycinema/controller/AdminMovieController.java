package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.ExternalMovieSearchResponse;
import com.kkst.mycinema.dto.ImportMovieRequest;
import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.service.MovieImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin controller for movie import operations from external sources like TMDb.
 * Only available when MovieImportService is enabled (tmdb.api.enabled=true).
 */
@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Admin - TMDb Movie Import", description = "Admin-only endpoints for searching and importing movies from The Movie Database (TMDb). Requires ROLE_ADMIN and valid JWT token.")
@ConditionalOnBean(MovieImportService.class)
public class AdminMovieController {

        private final MovieImportService movieImportService;

        /**
         * Search for movies from external data source (e.g., TMDb).
         *
         * @param query the search query
         * @param page  the page number (default: 1)
         * @return list of external movie search results
         */
        @GetMapping("/search")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Search Movies from TMDb", description = "Search for movies in The Movie Database (TMDb) by title, keyword, or actor name. "
                        +
                        "This endpoint allows admins to browse TMDb's extensive movie catalog before importing them into the cinema system.\n\n"
                        +
                        "**Requirements:**\n" +
                        "- Admin role (ROLE_ADMIN)\n" +
                        "- Valid JWT token\n" +
                        "- TMDb API enabled (tmdb.api.enabled=true)\n\n" +
                        "**Examples:**\n" +
                        "- Search for \"Inception\"\n" +
                        "- Search for \"Tom Hanks\" to find movies\n" +
                        "- Search for \"Marvel\" to find superhero movies\n\n" +
                        "**Tip:** Use page parameter to browse through results (20 movies per page)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Search completed successfully (may return empty list if no results)", content = @Content(schema = @Schema(implementation = ExternalMovieSearchResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid search parameters (query too short or invalid page number)"),
                        @ApiResponse(responseCode = "401", description = "Not authenticated - valid JWT token required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
                        @ApiResponse(responseCode = "502", description = "External API error - TMDb service unavailable or API key invalid")
        })
        public ResponseEntity<List<ExternalMovieSearchResponse>> searchMovies(
                        @RequestParam @NotBlank(message = "Search query cannot be empty") @io.swagger.v3.oas.annotations.Parameter(description = "Search query (movie title, actor name, or keyword)", example = "Inception", required = true) String query,

                        @RequestParam(defaultValue = "1") @Positive @io.swagger.v3.oas.annotations.Parameter(description = "Page number for pagination (20 results per page)", example = "1") int page) {

                // Generate correlation ID for request tracing
                String correlationId = UUID.randomUUID().toString();
                log.info("[{}] Admin searching external movies: query='{}', page={}", correlationId, query, page);

                List<ExternalMovieSearchResponse> results = movieImportService.searchMovies(query, page);

                log.info("[{}] Search completed: found {} results", correlationId, results.size());

                // Add custom headers for better API standards
                HttpHeaders headers = new HttpHeaders();
                headers.add("X-Total-Count", String.valueOf(results.size()));
                headers.add("X-Correlation-Id", correlationId);

                // Return 200 OK with results (empty list is valid response)
                return ResponseEntity.ok()
                                .headers(headers)
                                .body(results);
        }

        @GetMapping("/tmdb/now-playing")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Get Now Playing Movies", description = "Get movies currently in theaters.")
        public ResponseEntity<List<ExternalMovieSearchResponse>> getNowPlaying(
                        @RequestParam(defaultValue = "1") int page) {
                var movies = movieImportService.getNowPlayingMovies(page);
                return ResponseEntity.ok(movies);
        }

        @GetMapping("/tmdb/upcoming")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Get Upcoming Movies", description = "Get upcoming movies.")
        public ResponseEntity<List<ExternalMovieSearchResponse>> getUpcoming(
                        @RequestParam(defaultValue = "1") int page) {
                var movies = movieImportService.getUpcomingMovies(page);
                return ResponseEntity.ok(movies);
        }

        /**
         * Import a movie from external data source into the local database.
         *
         * @param request the import request containing external ID and optional genre
         * @return the imported movie
         */
        @PostMapping("/import")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Import Movie from TMDb", description = "Import a movie from The Movie Database (TMDb) into the cinema's local database. "
                        +
                        "After importing, the movie becomes available for creating shows and bookings.\n\n" +
                        "**Requirements:**\n" +
                        "- Admin role (ROLE_ADMIN)\n" +
                        "- Valid JWT token\n" +
                        "- Valid TMDb movie ID (from search results)\n" +
                        "- TMDb API enabled\n\n" +
                        "**Workflow:**\n" +
                        "1. Search for movies using `/api/admin/movies/search`\n" +
                        "2. Copy the `externalId` from search results\n" +
                        "3. Import the movie using this endpoint\n" +
                        "4. Movie is now available in your cinema database\n" +
                        "5. Create shows using `/api/admin/shows`\n\n" +
                        "**Note:** You can optionally override the genre during import.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Movie imported successfully", content = @Content(schema = @Schema(implementation = MovieResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request - external ID required"),
                        @ApiResponse(responseCode = "401", description = "Not authenticated - valid JWT token required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
                        @ApiResponse(responseCode = "404", description = "Movie not found in external source with provided ID"),
                        @ApiResponse(responseCode = "502", description = "External API error - TMDb service unavailable")
        })
        public ResponseEntity<MovieResponse> importMovie(@Valid @RequestBody ImportMovieRequest request) {

                String correlationId = UUID.randomUUID().toString();
                log.info("[{}] Admin importing movie: externalId={}, genre={}",
                                correlationId, request.externalId(), request.genre());

                MovieResponse movie = movieImportService.importMovie(request);

                log.info("[{}] Movie imported successfully: id={}, title='{}'",
                                correlationId, movie.id(), movie.title());

                HttpHeaders headers = new HttpHeaders();
                headers.add("X-Correlation-Id", correlationId);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .headers(headers)
                                .body(movie);
        }

        private final com.kkst.mycinema.service.MovieService movieService;

        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Create Movie Manually", description = "Create a new movie manually without importing from external source")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Movie created successfully", content = @Content(schema = @Schema(implementation = MovieResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
        })
        public ResponseEntity<MovieResponse> createMovie(
                        @Valid @RequestBody com.kkst.mycinema.dto.CreateMovieRequest request) {
                MovieResponse movie = movieService.createMovie(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(movie);
        }
}
