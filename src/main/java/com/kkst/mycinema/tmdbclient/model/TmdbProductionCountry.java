package com.kkst.mycinema.tmdbclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a production country in TMDb.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbProductionCountry {

    @JsonProperty("iso_3166_1")
    private String iso31661;

    @JsonProperty("name")
    private String name;
}

