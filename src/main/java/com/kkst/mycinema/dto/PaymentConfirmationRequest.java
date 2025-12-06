package com.kkst.mycinema.dto;

import com.kkst.mycinema.payment.PaymentRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record PaymentConfirmationRequest(
        @NotBlank(message = "Hold token is required")
        String holdToken,

        @NotNull(message = "Payment method is required")
        PaymentRequest.PaymentMethod paymentMethod,

        // Optional card details (required if paymentMethod is CARD)
        String cardNumber,
        String cardExpiry,
        String cardCvv,
        String cardHolderName
) {}

