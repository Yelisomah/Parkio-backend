package com.example.parkio.dto.request;

import com.example.parkio.entity.ParkingSpot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParkingSpotRequest(

        @NotBlank(message = "Spot number is required")
        @Size(max = 20)
        String spotNumber,

        @NotNull(message = "Spot type is required")
        ParkingSpot.SpotType type,

        @Size(max = 200)
        String notes
) {}
