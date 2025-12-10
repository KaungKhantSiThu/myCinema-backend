package com.kkst.mycinema.tmdbclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents detailed movie information from TMDb.
 * Contains full movie details including runtime, budget, revenue, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbMovieDetails {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("overview")
    private String overview;

    @JsonProperty("tagline")
    private String tagline;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("runtime")
    private Integer runtime;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("genres")
    private List<TmdbGenre> genres;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    @JsonProperty("popularity")
    private Double popularity;

    @JsonProperty("budget")
    private Long budget;

    @JsonProperty("revenue")
    private Long revenue;

    @JsonProperty("status")
    private String status;

    @JsonProperty("adult")
    private Boolean adult;

    @JsonProperty("video")
    private Boolean video;

    @JsonProperty("homepage")
    private String homepage;

    @JsonProperty("imdb_id")
    private String imdbId;

    @JsonProperty("production_companies")
    private List<TmdbProductionCompany> productionCompanies;

    @JsonProperty("production_countries")
    private List<TmdbProductionCountry> productionCountries;

    @JsonProperty("spoken_languages")
    private List<TmdbSpokenLanguage> spokenLanguages;
}

