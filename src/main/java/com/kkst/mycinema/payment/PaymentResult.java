package com.kkst.mycinema.payment;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Result of a payment or refund operation.
 */
@Getter
@Builder
public class PaymentResult {
    private final boolean success;
    private final String transactionId;
    private final PaymentStatus status;
    private final String message;
    private final String errorCode;
    private final BigDecimal amount;
    private final String currency;
    private final LocalDateTime processedAt;
    private final String gatewayReference;
    private final String receiptUrl;

    public static PaymentResult success(String transactionId, BigDecimal amount, String message) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .status(PaymentStatus.COMPLETED)
                .amount(amount)
                .message(message)
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static PaymentResult pending(String transactionId, BigDecimal amount, String message) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .status(PaymentStatus.PENDING)
                .amount(amount)
                .message(message)
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static PaymentResult failure(String errorCode, String message) {
        return PaymentResult.builder()
                .success(false)
                .status(PaymentStatus.FAILED)
                .errorCode(errorCode)
                .message(message)
                .processedAt(LocalDateTime.now())
                .build();
    }
}

