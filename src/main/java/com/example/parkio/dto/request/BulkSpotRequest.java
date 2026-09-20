package com.example.parkio.dto.request;

import com.example.parkio.entity.ParkingSpot;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulkSpotRequest(

        @NotBlank(message = "Prefix is required, e.g. 'A-'")
        @Size(max = 10)
        String prefix,

        @NotNull @Min(1) @Max(500)
        Integer count,

        @NotNull(message = "Spot type is required")
        ParkingSpot.SpotType type,

        @Min(1) @Max(5)
        Integer padWidth
) {
    public BulkSpotRequest {
        if (padWidth == null) padWidth = 2;
    }
}
