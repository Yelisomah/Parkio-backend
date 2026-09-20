package com.example.parkio.dto.request;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotNull(message = "Booking ID is required")
        Long bookingId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Minimum rating is 1")
        @Max(value = 5, message = "Maximum rating is 5")
        Integer rating,

        @Size(max = 1000, message = "Comment must not exceed 1000 characters")
        String comment
) {}
