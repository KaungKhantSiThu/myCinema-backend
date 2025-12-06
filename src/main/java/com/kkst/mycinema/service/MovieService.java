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

    public Page<MovieResponse> getMoviesPaginated(Pageable pageable) {
        return movieRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private MovieResponse mapToResponse(com.kkst.mycinema.entity.Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .durationMinutes(movie.getDurationMinutes())
                .genre(movie.getGenre())
                .build();
    }
}


