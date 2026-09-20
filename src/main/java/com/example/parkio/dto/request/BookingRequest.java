package com.example.parkio.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingRequest(

        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,

        @NotNull(message = "Spot ID is required")
        Long spotId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        LocalDateTime endTime,

        String notes,

        /** Optional promo code — validated and applied against discountAmount. */
        String promoCode
) {}
