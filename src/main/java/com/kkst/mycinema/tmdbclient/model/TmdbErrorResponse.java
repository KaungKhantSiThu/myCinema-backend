package com.kkst.mycinema.tmdbclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an error response from TMDb API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbErrorResponse {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("status_code")
    private Integer statusCode;

    @JsonProperty("status_message")
    private String statusMessage;
}

