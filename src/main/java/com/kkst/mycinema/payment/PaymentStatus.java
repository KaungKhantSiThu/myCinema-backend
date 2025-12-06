package com.kkst.mycinema.payment;

/**
 * Possible states of a payment transaction.
 */
public enum PaymentStatus {
    PENDING,      // Payment initiated but not yet confirmed
    PROCESSING,   // Payment is being processed
    COMPLETED,    // Payment successfully completed
    FAILED,       // Payment failed
    CANCELLED,    // Payment was cancelled
    REFUNDED,     // Payment was fully refunded
    PARTIALLY_REFUNDED  // Payment was partially refunded
}

