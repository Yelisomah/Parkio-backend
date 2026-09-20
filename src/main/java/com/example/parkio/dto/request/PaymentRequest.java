package com.example.parkio.dto.request;

import com.example.parkio.entity.Payment;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(

        @NotNull(message = "Booking ID is required")
        Long bookingId,

        @NotNull(message = "Payment method is required")
        Payment.PaymentMethod paymentMethod,

        // In a real integration this would carry a gateway token / nonce
        String gatewayToken
) {}
