package com.kkst.mycinema.payment;

import java.math.BigDecimal;

/**
 * Strategy interface for payment processing.
 * Allows easy swapping between different payment providers.
 */
public interface PaymentGateway {

    /**
     * Process a payment
     * @param paymentRequest the payment details
     * @return the payment result
     */
    PaymentResult processPayment(PaymentRequest paymentRequest);

    /**
     * Process a refund
     * @param transactionId the original transaction ID
     * @param amount the amount to refund
     * @return the refund result
     */
    PaymentResult processRefund(String transactionId, BigDecimal amount);

    /**
     * Verify a payment status
     * @param transactionId the transaction ID to verify
     * @return the current payment status
     */
    PaymentStatus verifyPayment(String transactionId);

    /**
     * Get the name of this payment gateway
     */
    String getGatewayName();
}

