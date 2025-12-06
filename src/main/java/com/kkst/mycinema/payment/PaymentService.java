package com.kkst.mycinema.payment;

import com.kkst.mycinema.entity.Booking;
import com.kkst.mycinema.exception.PaymentFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment service that coordinates payment processing.
 * Uses the Strategy pattern via PaymentGateway interface to support multiple
 * providers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentGateway paymentGateway;

    /**
     * Process payment for a booking.
     * 
     * @param referenceId Unique identifier for the payment (e.g. holdToken or
     *                    bookingId)
     */
    public PaymentResult processBookingPayment(Booking booking, String referenceId,
            PaymentRequest.PaymentMethod method) {
        log.info("Processing payment for ref: {} using {}", referenceId, paymentGateway.getGatewayName());

        var request = PaymentRequest.builder()
                .orderId(referenceId)
                .amount(booking.getTotalAmount())
                .currency("USD")
                .customerEmail(booking.getUser().getEmail())
                .customerName(booking.getUser().getName())
                .description("Cinema booking #" + booking.getId() + " - " +
                        booking.getShow().getMovie().getTitle())
                .paymentMethod(method)
                .metadata(Map.of(
                        "booking_id", String.valueOf(booking.getId()),
                        "show_id", String.valueOf(booking.getShow().getId()),
                        "movie", booking.getShow().getMovie().getTitle()))
                .build();

        var result = paymentGateway.processPayment(request);

        if (!result.isSuccess()) {
            log.error("Payment failed for booking {}: {}", booking.getId(), result.getMessage());
            throw new PaymentFailedException(result.getErrorCode(), result.getMessage());
        }

        log.info("Payment successful for booking {}. Transaction: {}",
                booking.getId(), result.getTransactionId());
        return result;
    }

    /**
     * Process refund for a cancelled booking.
     */
    public PaymentResult processRefund(String transactionId, BigDecimal amount) {
        log.info("Processing refund for transaction {} using {}",
                transactionId, paymentGateway.getGatewayName());

        var result = paymentGateway.processRefund(transactionId, amount);

        if (!result.isSuccess()) {
            log.error("Refund failed for transaction {}: {}", transactionId, result.getMessage());
            throw new PaymentFailedException(result.getErrorCode(), result.getMessage());
        }

        log.info("Refund successful for transaction {}. Refund ID: {}",
                transactionId, result.getTransactionId());
        return result;
    }

    /**
     * Verify payment status.
     */
    public PaymentStatus verifyPayment(String transactionId) {
        return paymentGateway.verifyPayment(transactionId);
    }

    /**
     * Get the active payment gateway name.
     */
    public String getActiveGateway() {
        return paymentGateway.getGatewayName();
    }
}
