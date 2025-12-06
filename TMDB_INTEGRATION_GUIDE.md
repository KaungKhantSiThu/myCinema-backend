# TMDb Integration Implementation Guide

## Overview
This guide details how to integrate The Movie Database (TMDb) API using the `themoviedbapi` library to replace mock movie data with real movie information.

## Prerequisites

1. **Get TMDb API Key:**
   - Register at https://www.themoviedb.org/signup
   - Go to Settings → API → Request an API Key
   - Choose "Developer" option
   - Save your API key (v3 auth)

2. **Add Maven Dependency:**
   Add to `pom.xml`:
   ```xml
   <!-- TMDb API Client -->
   <dependency>
       <groupId>com.github.c-eg</groupId>
       <artifactId>themoviedbapi</artifactId>
       <version>2.0.2</version>
   </dependency>
   ```

## Implementation Steps

### Step 1: Enhanced Movie Entity

Update `Movie.java` with TMDb fields:

```java
@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TMDb reference (nullable for manually created movies)
    @Column(name = "tmdb_id", unique = true)
    private Integer tmdbId;

    // Core info
    @Column(nullable = false)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // Rich metadata from TMDb
    @Column(length = 2000)
    private String overview;

    @Column(length = 200)
    private String tagline;

    @Column(name = "poster_path", length = 200)
    private String posterPath;

    @Column(name = "backdrop_path", length = 200)
    private String backdropPath;

    @Column(name = "vote_average")
    private Double voteAverage;

    @Column(name = "vote_count")
    private Integer voteCount;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "original_language", length = 10)
    private String originalLanguage;

    // Genres (stored as JSON string or comma-separated)
    @Column(length = 500)
    private String genres;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "tmdb_synced_at")
    private LocalDateTime tmdbSyncedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Step 2: Database Migration

Create `V3__enhance_movies_table.sql`:

```sql
-- V3: Enhance movies table with TMDb fields
ALTER TABLE movies ADD COLUMN IF NOT EXISTS tmdb_id INTEGER UNIQUE;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS overview VARCHAR(2000);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS tagline VARCHAR(200);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS poster_path VARCHAR(200);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS backdrop_path VARCHAR(200);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS vote_average DECIMAL(3,1);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS vote_count INTEGER;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS release_date DATE;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS original_language VARCHAR(10);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS genres VARCHAR(500);
ALTER TABLE movies ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE movies ADD COLUMN IF NOT EXISTS tmdb_synced_at TIMESTAMP;

-- Index for TMDb lookups
CREATE INDEX IF NOT EXISTS idx_movies_tmdb_id ON movies(tmdb_id);
```

### Step 3: Configuration

Add to `application.properties`:
```properties
# TMDb API Configuration
tmdb.api.key=${TMDB_API_KEY:your-default-key-for-dev}
tmdb.api.language=en-US
tmdb.image.base-url=https://image.tmdb.org/t/p/
tmdb.image.poster-size=w500
tmdb.image.backdrop-size=w1280
```

### Step 4: TMDb Configuration Bean

Create `TmdbConfig.java`:

```java
package com.kkst.mycinema.config;

import info.movito.themoviedbapi.TmdbApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TmdbConfig {

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Bean
    public TmdbApi tmdbApi() {
        return new TmdbApi(apiKey);
    }
}
```

### Step 5: DTOs for TMDb

Create `TmdbMovieDto.java`:

```java
package com.kkst.mycinema.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

@Builder
public record TmdbMovieDto(
    Integer tmdbId,
    String title,
    String overview,
    String tagline,
    Integer runtime,
    String posterPath,
    String backdropPath,
    Double voteAverage,
    Integer voteCount,
    LocalDate releaseDate,
    String originalLanguage,
    List<String> genres
) {
    // Helper to get full poster URL
    public String getFullPosterUrl(String baseUrl, String size) {
        return posterPath != null ? baseUrl + size + posterPath : null;
    }
    
    // Helper to get full backdrop URL
    public String getFullBackdropUrl(String baseUrl, String size) {
        return backdropPath != null ? baseUrl + size + backdropPath : null;
    }
}
```

Update `MovieResponse.java`:

```java
package com.kkst.mycinema.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

@Builder
public record MovieResponse(
    Long id,
    Integer tmdbId,
    String title,
    Integer durationMinutes,
    String overview,
    String tagline,
    String posterUrl,
    String backdropUrl,
    Double voteAverage,
    Integer voteCount,
    LocalDate releaseDate,
    String originalLanguage,
    List<String> genres
) {}
```

### Step 6: TMDb Service

Create `TmdbService.java`:

```java
package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.TmdbMovieDto;
import com.kkst.mycinema.entity.Movie;
import com.kkst.mycinema.exception.MovieNotFoundException;
import com.kkst.mycinema.repository.MovieRepository;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.movies.MovieDb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmdbService {

    private final TmdbApi tmdbApi;
    private final MovieRepository movieRepository;

    @Value("${tmdb.api.language:en-US}")
    private String language;

    @Value("${tmdb.image.base-url}")
    private String imageBaseUrl;

    @Value("${tmdb.image.poster-size}")
    private String posterSize;

    @Value("${tmdb.image.backdrop-size}")
    private String backdropSize;

    /**
     * Search movies from TMDb API
     */
    public List<TmdbMovieDto> searchMovies(String query, int page) {
        log.info("Searching TMDb for: {}", query);
        
        MovieResultsPage results = tmdbApi.getSearch()
            .searchMovie(query, false, language, null, page);
        
        return results.getResults().stream()
            .map(this::mapToTmdbDto)
            .toList();
    }

    /**
     * Get popular movies from TMDb
     */
    public List<TmdbMovieDto> getPopularMovies(int page) {
        log.info("Fetching popular movies from TMDb");
        
        MovieResultsPage results = tmdbApi.getMovies()
            .getPopularMovies(language, page);
        
        return results.getResults().stream()
            .map(this::mapToTmdbDto)
            .toList();
    }

    /**
     * Get now playing movies from TMDb
     */
    public List<TmdbMovieDto> getNowPlayingMovies(int page) {
        log.info("Fetching now playing movies from TMDb");
        
        MovieResultsPage results = tmdbApi.getMovies()
            .getNowPlayingMovies(language, page, null);
        
        return results.getResults().stream()
            .map(this::mapToTmdbDto)
            .toList();
    }

    /**
     * Get upcoming movies from TMDb
     */
    public List<TmdbMovieDto> getUpcomingMovies(int page) {
        log.info("Fetching upcoming movies from TMDb");
        
        MovieResultsPage results = tmdbApi.getMovies()
            .getUpcoming(language, page, null);
        
        return results.getResults().stream()
            .map(this::mapToTmdbDto)
            .toList();
    }

    /**
     * Get detailed movie info from TMDb
     */
    public TmdbMovieDto getMovieDetails(int tmdbId) {
        log.info("Fetching movie details for TMDb ID: {}", tmdbId);
        
        MovieDb movie = tmdbApi.getMovies().getMovie(tmdbId, language);
        return mapToDetailedTmdbDto(movie);
    }

    /**
     * Import a movie from TMDb to local database
     */
    @Transactional
    @CacheEvict(value = "movies", allEntries = true)
    public Movie importFromTmdb(int tmdbId) {
        log.info("Importing movie from TMDb ID: {}", tmdbId);
        
        // Check if already imported
        Optional<Movie> existing = movieRepository.findByTmdbId(tmdbId);
        if (existing.isPresent()) {
            log.info("Movie already exists with TMDb ID: {}", tmdbId);
            return existing.get();
        }
        
        // Fetch detailed info from TMDb
        MovieDb tmdbMovie = tmdbApi.getMovies().getMovie(tmdbId, language);
        
        if (tmdbMovie == null) {
            throw new MovieNotFoundException("Movie not found on TMDb with ID: " + tmdbId);
        }
        
        // Map and save
        Movie movie = Movie.builder()
            .tmdbId(tmdbId)
            .title(tmdbMovie.getTitle())
            .durationMinutes(tmdbMovie.getRuntime() != null ? tmdbMovie.getRuntime() : 120)
            .overview(tmdbMovie.getOverview())
            .tagline(tmdbMovie.getTagline())
            .posterPath(tmdbMovie.getPosterPath())
            .backdropPath(tmdbMovie.getBackdropPath())
            .voteAverage(tmdbMovie.getVoteAverage())
            .voteCount(tmdbMovie.getVoteCount())
            .releaseDate(parseReleaseDate(tmdbMovie.getReleaseDate()))
            .originalLanguage(tmdbMovie.getOriginalLanguage())
            .genres(extractGenres(tmdbMovie))
            .tmdbSyncedAt(LocalDateTime.now())
            .build();
        
        movie = movieRepository.save(movie);
        log.info("Imported movie: {} (ID: {})", movie.getTitle(), movie.getId());
        
        return movie;
    }

    /**
     * Sync/update an existing movie with TMDb data
     */
    @Transactional
    @CacheEvict(value = "movies", allEntries = true)
    public Movie syncWithTmdb(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new MovieNotFoundException(movieId));
        
        if (movie.getTmdbId() == null) {
            throw new IllegalStateException("Movie has no TMDb ID to sync");
        }
        
        MovieDb tmdbMovie = tmdbApi.getMovies().getMovie(movie.getTmdbId(), language);
        
        // Update fields
        movie.setTitle(tmdbMovie.getTitle());
        movie.setOverview(tmdbMovie.getOverview());
        movie.setTagline(tmdbMovie.getTagline());
        movie.setPosterPath(tmdbMovie.getPosterPath());
        movie.setBackdropPath(tmdbMovie.getBackdropPath());
        movie.setVoteAverage(tmdbMovie.getVoteAverage());
        movie.setVoteCount(tmdbMovie.getVoteCount());
        if (tmdbMovie.getRuntime() != null) {
            movie.setDurationMinutes(tmdbMovie.getRuntime());
        }
        movie.setTmdbSyncedAt(LocalDateTime.now());
        
        return movieRepository.save(movie);
    }

    // Helper methods
    
    private TmdbMovieDto mapToTmdbDto(info.movito.themoviedbapi.model.core.Movie movie) {
        return TmdbMovieDto.builder()
            .tmdbId(movie.getId())
            .title(movie.getTitle())
            .overview(movie.getOverview())
            .posterPath(movie.getPosterPath())
            .backdropPath(movie.getBackdropPath())
            .voteAverage(movie.getVoteAverage())
            .voteCount(movie.getVoteCount())
            .releaseDate(parseReleaseDate(movie.getReleaseDate()))
            .originalLanguage(movie.getOriginalLanguage())
            .build();
    }

    private TmdbMovieDto mapToDetailedTmdbDto(MovieDb movie) {
        return TmdbMovieDto.builder()
            .tmdbId(movie.getId())
            .title(movie.getTitle())
            .overview(movie.getOverview())
            .tagline(movie.getTagline())
            .runtime(movie.getRuntime())
            .posterPath(movie.getPosterPath())
            .backdropPath(movie.getBackdropPath())
            .voteAverage(movie.getVoteAverage())
            .voteCount(movie.getVoteCount())
            .releaseDate(parseReleaseDate(movie.getReleaseDate()))
            .originalLanguage(movie.getOriginalLanguage())
            .genres(movie.getGenres().stream()
                .map(g -> g.getName())
                .toList())
            .build();
    }

    private LocalDate parseReleaseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse release date: {}", dateStr);
            return null;
        }
    }

    private String extractGenres(MovieDb movie) {
        if (movie.getGenres() == null || movie.getGenres().isEmpty()) {
            return null;
        }
        return movie.getGenres().stream()
            .map(g -> g.getName())
            .collect(Collectors.joining(", "));
    }

    // Image URL builders
    
    public String getPosterUrl(String posterPath) {
        return posterPath != null ? imageBaseUrl + posterSize + posterPath : null;
    }

    public String getBackdropUrl(String backdropPath) {
        return backdropPath != null ? imageBaseUrl + backdropSize + backdropPath : null;
    }
}
```

### Step 7: Update Movie Repository

Add to `MovieRepository.java`:

```java
@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    Optional<Movie> findByTmdbId(Integer tmdbId);
    
    boolean existsByTmdbId(Integer tmdbId);
    
    List<Movie> findByTitleContainingIgnoreCase(String title);
    
    List<Movie> findByGenresContainingIgnoreCase(String genre);
}
```

### Step 8: Admin Controller for TMDb

Add to `AdminController.java`:

```java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final TmdbService tmdbService;

    // ... existing endpoints ...

    // TMDb Integration Endpoints
    
    @GetMapping("/tmdb/search")
    @Operation(summary = "Search movies on TMDb")
    public ResponseEntity<List<TmdbMovieDto>> searchTmdb(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.searchMovies(query, page));
    }

    @GetMapping("/tmdb/popular")
    @Operation(summary = "Get popular movies from TMDb")
    public ResponseEntity<List<TmdbMovieDto>> getPopularTmdb(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getPopularMovies(page));
    }

    @GetMapping("/tmdb/now-playing")
    @Operation(summary = "Get now playing movies from TMDb")
    public ResponseEntity<List<TmdbMovieDto>> getNowPlayingTmdb(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getNowPlayingMovies(page));
    }

    @GetMapping("/tmdb/{tmdbId}")
    @Operation(summary = "Get movie details from TMDb")
    public ResponseEntity<TmdbMovieDto> getTmdbMovieDetails(@PathVariable int tmdbId) {
        return ResponseEntity.ok(tmdbService.getMovieDetails(tmdbId));
    }

    @PostMapping("/tmdb/import/{tmdbId}")
    @Operation(summary = "Import a movie from TMDb to local database")
    public ResponseEntity<MovieResponse> importFromTmdb(@PathVariable int tmdbId) {
        Movie movie = tmdbService.importFromTmdb(tmdbId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(movie));
    }

    @PostMapping("/movies/{movieId}/sync-tmdb")
    @Operation(summary = "Sync existing movie with TMDb data")
    public ResponseEntity<MovieResponse> syncWithTmdb(@PathVariable Long movieId) {
        Movie movie = tmdbService.syncWithTmdb(movieId);
        return ResponseEntity.ok(mapToResponse(movie));
    }
    
    private MovieResponse mapToResponse(Movie movie) {
        return MovieResponse.builder()
            .id(movie.getId())
            .tmdbId(movie.getTmdbId())
            .title(movie.getTitle())
            .durationMinutes(movie.getDurationMinutes())
            .overview(movie.getOverview())
            .tagline(movie.getTagline())
            .posterUrl(tmdbService.getPosterUrl(movie.getPosterPath()))
            .backdropUrl(tmdbService.getBackdropUrl(movie.getBackdropPath()))
            .voteAverage(movie.getVoteAverage())
            .voteCount(movie.getVoteCount())
            .releaseDate(movie.getReleaseDate())
            .originalLanguage(movie.getOriginalLanguage())
            .genres(movie.getGenres() != null 
                ? Arrays.asList(movie.getGenres().split(", ")) 
                : null)
            .build();
    }
}
```

### Step 9: Update MovieService

Update `MovieService.java` to return rich movie data:

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;

    @Cacheable(CacheConfig.MOVIES_CACHE)
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    public MovieResponse getMovieById(Long id) {
        return movieRepository.findById(id)
            .map(this::mapToResponse)
            .orElseThrow(() -> new MovieNotFoundException(id));
    }

    private MovieResponse mapToResponse(Movie movie) {
        return MovieResponse.builder()
            .id(movie.getId())
            .tmdbId(movie.getTmdbId())
            .title(movie.getTitle())
            .durationMinutes(movie.getDurationMinutes())
            .overview(movie.getOverview())
            .tagline(movie.getTagline())
            .posterUrl(tmdbService.getPosterUrl(movie.getPosterPath()))
            .backdropUrl(tmdbService.getBackdropUrl(movie.getBackdropPath()))
            .voteAverage(movie.getVoteAverage())
            .voteCount(movie.getVoteCount())
            .releaseDate(movie.getReleaseDate())
            .originalLanguage(movie.getOriginalLanguage())
            .genres(movie.getGenres() != null 
                ? Arrays.asList(movie.getGenres().split(", ")) 
                : null)
            .build();
    }
}
```

## Testing

### TMDb Service Test

```java
@ExtendWith(MockitoExtension.class)
class TmdbServiceTest {

    @Mock
    private TmdbApi tmdbApi;
    
    @Mock
    private MovieRepository movieRepository;
    
    @InjectMocks
    private TmdbService tmdbService;

    @Test
    void importFromTmdb_NewMovie_Success() {
        // Test implementation
    }

    @Test
    void importFromTmdb_AlreadyExists_ReturnsExisting() {
        // Test implementation
    }
}
```

## Environment Variables

Add to `.env.example`:
```bash
# TMDb API Configuration
TMDB_API_KEY=your-tmdb-api-key-here
```

## Usage Examples

### Admin Workflow

1. **Search for a movie:**
   ```bash
   GET /api/admin/tmdb/search?query=Inception
   ```

2. **View details:**
   ```bash
   GET /api/admin/tmdb/27205
   ```

3. **Import to local database:**
   ```bash
   POST /api/admin/tmdb/import/27205
   ```

4. **Create show with imported movie:**
   ```bash
   POST /api/admin/shows
   {
     "movieId": 1,
     "hallId": 1,
     "startTime": "2025-12-05T19:00:00",
     "endTime": "2025-12-05T21:30:00"
   }
   ```

## Benefits

1. **Rich Movie Data:** Posters, descriptions, ratings
2. **Professional Look:** Real movie images
3. **Up-to-date Info:** Sync with TMDb anytime
4. **Searchable Catalog:** Easy to find and add movies
5. **Admin Control:** Import only what you need

