package com.kkst.mycinema.tmdbclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a genre in TMDb.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbGenre {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;
}

