package com.kkst.mycinema.tmdbclient.exception;

/**
 * Base exception for TMDb API errors.
 */
public class TmdbApiException extends RuntimeException {

    private final Integer statusCode;
    private final String statusMessage;

    public TmdbApiException(String message) {
        super(message);
        this.statusCode = null;
        this.statusMessage = null;
    }

    public TmdbApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.statusMessage = null;
    }

    public TmdbApiException(Integer statusCode, String statusMessage) {
        super(String.format("TMDb API error [%d]: %s", statusCode, statusMessage));
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public TmdbApiException(Integer statusCode, String statusMessage, Throwable cause) {
        super(String.format("TMDb API error [%d]: %s", statusCode, statusMessage), cause);
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}

