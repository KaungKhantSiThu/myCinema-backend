package com.kkst.mycinema.payment;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment request containing all necessary information for processing.
 */
@Getter
@Builder
public class PaymentRequest {
    private final String orderId;
    private final BigDecimal amount;
    private final String currency;
    private final String customerEmail;
    private final String customerName;
    private final String description;
    private final PaymentMethod paymentMethod;
    private final Map<String, String> metadata;

    // Card details (only for card payments)
    private final String cardNumber;
    private final String cardExpiry;
    private final String cardCvv;
    private final String cardHolderName;

    public enum PaymentMethod {
        CARD,
        BANK_TRANSFER,
        DIGITAL_WALLET,
        MOCK // For testing
    }
}

