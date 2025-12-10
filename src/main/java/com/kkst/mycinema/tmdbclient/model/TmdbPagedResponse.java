package com.kkst.mycinema.tmdbclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a paginated response from TMDb API.
 * Used for search results and other paginated endpoints.
 *
 * @param <T> The type of results in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbPagedResponse<T> {

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("results")
    private List<T> results;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("total_results")
    private Integer totalResults;
}

