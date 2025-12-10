package com.kkst.mycinema.tmdbclient.exception;

/**
 * Exception thrown when authentication with TMDb API fails.
 */
public class TmdbAuthenticationException extends TmdbApiException {

    public TmdbAuthenticationException(String message) {
        super(message);
    }

    public TmdbAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

