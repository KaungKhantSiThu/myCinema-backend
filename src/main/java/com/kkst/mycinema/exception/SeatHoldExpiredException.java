package com.kkst.mycinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a seat hold has expired or is invalid.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SeatHoldExpiredException extends RuntimeException {
    public SeatHoldExpiredException(String message) {
        super(message);
    }

    public SeatHoldExpiredException(String holdToken, boolean notFound) {
        super(notFound ? "Seat hold not found: " + holdToken : "Seat hold has expired: " + holdToken);
    }
}

