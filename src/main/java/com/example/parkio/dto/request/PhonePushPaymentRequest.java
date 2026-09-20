package com.example.parkio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PhonePushPaymentRequest(
        @NotBlank String phone,
        /** e.g. "mtn", "vodafone", "airteltigo" — defaults to "mtn" in the gateway if omitted. */
        String momoProvider
) {}
