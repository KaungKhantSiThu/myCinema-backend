package com.kkst.mycinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedBookingAccessException extends RuntimeException {
    public UnauthorizedBookingAccessException(String message) {
        super(message);
    }

    public UnauthorizedBookingAccessException() {
        super("You can only access your own bookings");
    }
}

