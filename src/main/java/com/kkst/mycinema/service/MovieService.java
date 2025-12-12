package com.kkst.mycinema.service;

import com.kkst.mycinema.config.CacheConfig;
import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;

    @Cacheable(CacheConfig.MOVIES_CACHE)
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public MovieResponse getMovieById(Long id) {
        return movieRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(
                        () -> new com.kkst.mycinema.exception.MovieNotFoundException("Movie not found with id: " + id));
    }

    public Page<MovieResponse> getMoviesPaginated(String query, String genre, Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<com.kkst.mycinema.entity.Movie> spec = (root, criteriaQuery,
                cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                String likePattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), likePattern));
            }

            if (genre != null && !genre.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("genre")), genre.trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return movieRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public MovieResponse createMovie(com.kkst.mycinema.dto.CreateMovieRequest request) {
        var movie = com.kkst.mycinema.entity.Movie.builder()
                .title(request.title())
                .durationMinutes(request.durationMinutes())
                .genre(request.genre())
                .description(request.description())
                .build();

        movie = movieRepository.save(movie);
        return mapToResponse(movie);
    }

    private MovieResponse mapToResponse(com.kkst.mycinema.entity.Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .durationMinutes(movie.getDurationMinutes())
                .genre(movie.getGenre())
                .description(movie.getDescription())
                .build();
    }
}
