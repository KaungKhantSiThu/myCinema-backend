package com.kkst.mycinema.service;

import com.kkst.mycinema.config.CacheConfig;
import com.kkst.mycinema.dto.MovieResponse;
import com.kkst.mycinema.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
                .map(movie -> MovieResponse.builder()
                        .id(movie.getId())
                        .title(movie.getTitle())
                        .durationMinutes(movie.getDurationMinutes())
                        .genre(movie.getGenre())
                        .build())
                .toList();
    }
}


