package com.kkst.mycinema.dto;

import com.kkst.mycinema.payment.PaymentRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "Request to confirm payment for reserved seats")
public record PaymentConfirmationRequest(
                @NotBlank(message = "Hold token is required") @Schema(description = "Seat hold token received from booking initiation", example = "HOLD-12345-ABCDE") String holdToken,

                @NotNull(message = "Payment method is required") @Schema(description = "Payment method", example = "CREDIT_CARD") PaymentRequest.PaymentMethod paymentMethod,

                // Optional card details (required if paymentMethod is CARD)
                @Schema(description = "Card number (required for CARD payment)", example = "4111111111111111") String cardNumber,

                @Schema(description = "Card expiry MM/YY", example = "12/25") String cardExpiry,

                @Schema(description = "Card CVV", example = "123") String cardCvv,

                @Schema(description = "Card holder name", example = "John Doe") String cardHolderName) {
}
