package com.kkst.mycinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a hall is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class HallNotFoundException extends RuntimeException {
    public HallNotFoundException(String message) {
        super(message);
    }

    public HallNotFoundException(Long hallId) {
        super("Hall not found with ID: " + hallId);
    }
}

