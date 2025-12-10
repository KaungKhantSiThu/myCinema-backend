package com.kkst.mycinema.tmdbclient.exception;

/**
 * Exception thrown when a requested resource is not found in TMDb.
 */
public class TmdbResourceNotFoundException extends TmdbApiException {

    public TmdbResourceNotFoundException(String message) {
        super(message);
    }

    public TmdbResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found with ID: %s", resourceType, resourceId));
    }
}

