package com.kkst.mycinema.exception;

public class BookingFailedAfterPaymentException extends RuntimeException {
    public BookingFailedAfterPaymentException(String message) {
        super(message);
    }

    public BookingFailedAfterPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
