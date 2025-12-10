package com.kkst.mycinema.tmdbclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a spoken language in TMDb.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbSpokenLanguage {

    @JsonProperty("iso_639_1")
    private String iso6391;

    @JsonProperty("name")
    private String name;

    @JsonProperty("english_name")
    private String englishName;
}

