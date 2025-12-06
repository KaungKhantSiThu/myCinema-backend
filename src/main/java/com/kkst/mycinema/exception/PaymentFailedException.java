package com.kkst.mycinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when payment processing fails.
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentFailedException extends RuntimeException {
    private final String errorCode;

    public PaymentFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentFailedException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}

