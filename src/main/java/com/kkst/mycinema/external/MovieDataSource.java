package com.kkst.mycinema.external;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction for external movie data sources.
 * This interface allows the system to be decoupled from specific movie data
 * providers.
 * Implementations can include TMDb, OMDB, or internal database sources.
 */
public interface MovieDataSource {

    /**
     * Search for movies by title.
     *
     * @param query the search query
     * @param page  the page number (1-based)
     * @return list of matching movies
     */
    List<ExternalMovieData> searchMovies(String query, int page);

    /**
     * Get now playing movies.
     * 
     * @param page the page number (1-based)
     * @return list of now playing movies
     */
    List<ExternalMovieData> getNowPlaying(int page);

    /**
     * Get upcoming movies.
     * 
     * @param page the page number (1-based)
     * @return list of upcoming movies
     */
    List<ExternalMovieData> getUpcoming(int page);

    /**
     * Get detailed movie information by external ID.
     *
     * @param externalId the external movie ID
     * @return movie data if found
     */
    Optional<ExternalMovieData> getMovieById(String externalId);

    /**
     * Get the name of this data source (e.g., "TMDb", "OMDB").
     *
     * @return data source name
     */
    String getSourceName();
}
