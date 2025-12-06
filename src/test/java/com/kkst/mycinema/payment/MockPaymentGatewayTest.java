package com.kkst.mycinema.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockPaymentGateway Tests")
class MockPaymentGatewayTest {

    private MockPaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        paymentGateway = new MockPaymentGateway();
        paymentGateway.clearTransactions();
    }

    @Test
    @DisplayName("Should process payment successfully")
    void processPayment_Success() {
        // Given
        var request = PaymentRequest.builder()
                .orderId("order_123")
                .amount(new BigDecimal("29.99"))
                .currency("USD")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .description("Cinema booking")
                .paymentMethod(PaymentRequest.PaymentMethod.CARD)
                .cardNumber("4242424242424242")
                .build();

        // When
        PaymentResult result = paymentGateway.processPayment(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).startsWith("mock_txn_");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(result.getMessage()).contains("successfully");
    }

    @Test
    @DisplayName("Should decline payment with specific card number")
    void processPayment_Declined() {
        // Given - Using the decline card number
        var request = PaymentRequest.builder()
                .orderId("order_124")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .customerEmail("test@example.com")
                .paymentMethod(PaymentRequest.PaymentMethod.CARD)
                .cardNumber("4000000000000002") // Fail card
                .build();

        // When
        PaymentResult result = paymentGateway.processPayment(request);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getErrorCode()).isEqualTo("CARD_DECLINED");
    }

    @Test
    @DisplayName("Should process refund successfully")
    void processRefund_Success() {
        // Given - First create a successful payment
        var paymentRequest = PaymentRequest.builder()
                .orderId("order_125")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerEmail("test@example.com")
                .paymentMethod(PaymentRequest.PaymentMethod.CARD)
                .build();

        PaymentResult paymentResult = paymentGateway.processPayment(paymentRequest);
        assertThat(paymentResult.isSuccess()).isTrue();

        // When - Process refund
        PaymentResult refundResult = paymentGateway.processRefund(
                paymentResult.getTransactionId(),
                new BigDecimal("100.00")
        );

        // Then
        assertThat(refundResult.isSuccess()).isTrue();
        assertThat(refundResult.getTransactionId()).startsWith("mock_refund_");
        assertThat(refundResult.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("Should fail refund for non-existent transaction")
    void processRefund_TransactionNotFound() {
        // When
        PaymentResult result = paymentGateway.processRefund(
                "non_existent_txn",
                new BigDecimal("50.00")
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("TRANSACTION_NOT_FOUND");
    }

    @Test
    @DisplayName("Should verify payment status")
    void verifyPayment_Success() {
        // Given
        var request = PaymentRequest.builder()
                .orderId("order_126")
                .amount(new BigDecimal("75.00"))
                .currency("USD")
                .customerEmail("test@example.com")
                .paymentMethod(PaymentRequest.PaymentMethod.CARD)
                .build();

        PaymentResult paymentResult = paymentGateway.processPayment(request);

        // When
        PaymentStatus status = paymentGateway.verifyPayment(paymentResult.getTransactionId());

        // Then
        assertThat(status).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should return failed status for unknown transaction")
    void verifyPayment_UnknownTransaction() {
        // When
        PaymentStatus status = paymentGateway.verifyPayment("unknown_txn");

        // Then
        assertThat(status).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Should return correct gateway name")
    void getGatewayName() {
        assertThat(paymentGateway.getGatewayName()).isEqualTo("MockPaymentGateway");
    }
}

