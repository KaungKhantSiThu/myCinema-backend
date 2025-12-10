package com.kkst.mycinema.tmdbclient.mapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps TMDb genre IDs to genre names.
 * TMDb returns genre IDs in search results and full genre objects in movie details.
 * This mapper provides a consistent way to convert genre IDs to names.
 * 
 * Based on TMDb API v3 genre list:
 * https://developers.themoviedb.org/3/genres/get-movie-list
 */
@Component
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbGenreMapper {

    /**
     * Official TMDb genre ID to name mapping.
     * This mapping is relatively stable but should be updated if TMDb changes their genre structure.
     */
    private static final Map<Integer, String> GENRE_MAP = Map.ofEntries(
        Map.entry(28, "Action"),
        Map.entry(12, "Adventure"),
        Map.entry(16, "Animation"),
        Map.entry(35, "Comedy"),
        Map.entry(80, "Crime"),
        Map.entry(99, "Documentary"),
        Map.entry(18, "Drama"),
        Map.entry(10751, "Family"),
        Map.entry(14, "Fantasy"),
        Map.entry(36, "History"),
        Map.entry(27, "Horror"),
        Map.entry(10402, "Music"),
        Map.entry(9648, "Mystery"),
        Map.entry(10749, "Romance"),
        Map.entry(878, "Science Fiction"),
        Map.entry(10770, "TV Movie"),
        Map.entry(53, "Thriller"),
        Map.entry(10752, "War"),
        Map.entry(37, "Western")
    );

    /**
     * Maps a TMDb genre ID to its corresponding genre name.
     *
     * @param genreId The TMDb genre ID
     * @return The genre name, or "Unknown" if the ID is not recognized
     */
    public String mapGenreIdToName(Integer genreId) {
        if (genreId == null) {
            return "Unknown";
        }
        return GENRE_MAP.getOrDefault(genreId, "Unknown");
    }

    /**
     * Maps a TMDb genre ID string to its corresponding genre name.
     *
     * @param genreIdStr The TMDb genre ID as a string
     * @return The genre name, or "Unknown" if the ID is not recognized or invalid
     */
    public String mapGenreIdToName(String genreIdStr) {
        if (genreIdStr == null || genreIdStr.isEmpty()) {
            return "Unknown";
        }
        try {
            Integer genreId = Integer.parseInt(genreIdStr);
            return mapGenreIdToName(genreId);
        } catch (NumberFormatException e) {
            return "Unknown";
        }
    }
}
