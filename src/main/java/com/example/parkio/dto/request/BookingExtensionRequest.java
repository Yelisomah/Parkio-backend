package com.example.parkio.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingExtensionRequest(
        @NotNull @Future(message = "newEndTime must be in the future") LocalDateTime newEndTime
) {}
