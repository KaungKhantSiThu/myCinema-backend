package com.kkst.mycinema.service;

import com.kkst.mycinema.entity.Booking;
import com.kkst.mycinema.entity.Movie;
import com.kkst.mycinema.entity.Show;
import com.kkst.mycinema.entity.User;
import com.kkst.mycinema.exception.PaymentFailedException;
import com.kkst.mycinema.payment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    private Booking testBooking;

    @BeforeEach
    void setUp() {
        Movie movie = Movie.builder().title("Inception").build();
        Show show = Show.builder().id(1L).movie(movie).build();
        User user = User.builder().email("test@example.com").name("Test User").build();

        testBooking = Booking.builder()
                .id(100L)
                .show(show)
                .user(user)
                .totalAmount(new BigDecimal("30.00"))
                .build();
    }

    @Test
    void processBookingPayment_Success() {
        // Arrange
        String referenceId = "ref-123";
        PaymentResult successResult = PaymentResult.builder()
                .success(true)
                .transactionId("tx-999")
                .build();

        when(paymentGateway.processPayment(any(PaymentRequest.class))).thenReturn(successResult);
        when(paymentGateway.getGatewayName()).thenReturn("TestGateway");

        // Act
        PaymentResult result = paymentService.processBookingPayment(testBooking, referenceId,
                PaymentRequest.PaymentMethod.CARD);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("tx-999", result.getTransactionId());
        verify(paymentGateway).processPayment(any(PaymentRequest.class));
    }

    @Test
    void processBookingPayment_Failure_ThrowsException() {
        // Arrange
        String referenceId = "ref-123";
        PaymentResult failedResult = PaymentResult.builder()
                .success(false)
                .errorCode("ERR_CARD_DECLINED")
                .message("Card declined")
                .build();

        when(paymentGateway.processPayment(any(PaymentRequest.class))).thenReturn(failedResult);
        when(paymentGateway.getGatewayName()).thenReturn("TestGateway");

        // Act & Assert
        PaymentFailedException exception = assertThrows(PaymentFailedException.class, () -> paymentService
                .processBookingPayment(testBooking, referenceId, PaymentRequest.PaymentMethod.CARD));

        assertEquals("Card declined", exception.getMessage());
        assertEquals("ERR_CARD_DECLINED", exception.getErrorCode());
    }

    @Test
    void processRefund_Success() {
        // Arrange
        String txId = "tx-999";
        BigDecimal amount = new BigDecimal("30.00");
        PaymentResult successResult = PaymentResult.builder()
                .success(true)
                .transactionId("refund-tx-111")
                .build();

        when(paymentGateway.processRefund(txId, amount)).thenReturn(successResult);
        when(paymentGateway.getGatewayName()).thenReturn("TestGateway");

        // Act
        PaymentResult result = paymentService.processRefund(txId, amount);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("refund-tx-111", result.getTransactionId());
    }

    @Test
    void processRefund_Failure_ThrowsException() {
        // Arrange
        String txId = "tx-999";
        BigDecimal amount = new BigDecimal("30.00");
        PaymentResult failedResult = PaymentResult.builder()
                .success(false)
                .errorCode("ERR_REFUND_FAILED")
                .message("Refund failed")
                .build();

        when(paymentGateway.processRefund(txId, amount)).thenReturn(failedResult);
        when(paymentGateway.getGatewayName()).thenReturn("TestGateway");

        // Act & Assert
        PaymentFailedException exception = assertThrows(PaymentFailedException.class,
                () -> paymentService.processRefund(txId, amount));

        assertEquals("Refund failed", exception.getMessage());
    }
}
