package com.kkst.mycinema.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock payment gateway for development and testing.
 * Simulates payment processing with configurable behaviors.
 *
 * Replace this with StripePaymentGateway or other real implementations in production.
 */
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    // In-memory store for tracking mock transactions
    private final Map<String, PaymentResult> transactions = new ConcurrentHashMap<>();

    // Configurable failure scenarios for testing
    private static final String FAIL_CARD_NUMBER = "4000000000000002";
    private static final String DECLINE_CARD_NUMBER = "4000000000000069";

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("[MOCK] Processing payment for order: {}, amount: {} {}",
                request.getOrderId(), request.getAmount(), request.getCurrency());

        // Simulate processing delay
        simulateNetworkDelay();

        // Check for test failure scenarios
        if (shouldSimulateFailure(request)) {
            var result = PaymentResult.failure("CARD_DECLINED", "Card was declined by the issuer");
            log.warn("[MOCK] Payment declined for order: {}", request.getOrderId());
            return result;
        }

        // Simulate successful payment
        String transactionId = "mock_txn_" + UUID.randomUUID().toString().substring(0, 8);
        var result = PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .status(PaymentStatus.COMPLETED)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .message("Payment processed successfully")
                .gatewayReference("mock_ref_" + System.currentTimeMillis())
                .receiptUrl("https://mock-payment.example.com/receipts/" + transactionId)
                .processedAt(java.time.LocalDateTime.now())
                .build();

        transactions.put(transactionId, result);
        log.info("[MOCK] Payment successful. Transaction ID: {}", transactionId);

        return result;
    }

    @Override
    public PaymentResult processRefund(String transactionId, BigDecimal amount) {
        log.info("[MOCK] Processing refund for transaction: {}, amount: {}", transactionId, amount);

        simulateNetworkDelay();

        // Check if original transaction exists
        if (!transactions.containsKey(transactionId)) {
            return PaymentResult.failure("TRANSACTION_NOT_FOUND",
                    "Original transaction not found: " + transactionId);
        }

        String refundId = "mock_refund_" + UUID.randomUUID().toString().substring(0, 8);
        var result = PaymentResult.builder()
                .success(true)
                .transactionId(refundId)
                .status(PaymentStatus.REFUNDED)
                .amount(amount)
                .message("Refund processed successfully")
                .gatewayReference("mock_refund_ref_" + System.currentTimeMillis())
                .processedAt(java.time.LocalDateTime.now())
                .build();

        log.info("[MOCK] Refund successful. Refund ID: {}", refundId);
        return result;
    }

    @Override
    public PaymentStatus verifyPayment(String transactionId) {
        log.info("[MOCK] Verifying payment status for: {}", transactionId);

        var result = transactions.get(transactionId);
        if (result == null) {
            log.warn("[MOCK] Transaction not found: {}", transactionId);
            return PaymentStatus.FAILED;
        }

        return result.getStatus();
    }

    @Override
    public String getGatewayName() {
        return "MockPaymentGateway";
    }

    private boolean shouldSimulateFailure(PaymentRequest request) {
        if (request.getCardNumber() == null) {
            return false;
        }
        return FAIL_CARD_NUMBER.equals(request.getCardNumber())
                || DECLINE_CARD_NUMBER.equals(request.getCardNumber());
    }

    private void simulateNetworkDelay() {
        try {
            // Simulate 100-500ms network delay
            Thread.sleep(100 + (long) (Math.random() * 400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Test helper methods
    public void clearTransactions() {
        transactions.clear();
    }

    public int getTransactionCount() {
        return transactions.size();
    }
}

