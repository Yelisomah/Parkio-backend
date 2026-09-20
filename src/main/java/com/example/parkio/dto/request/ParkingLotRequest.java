package com.example.parkio.dto.request;

import com.example.parkio.entity.ParkingLot;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ParkingLotRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Address is required")
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100)
        String state,

        @Size(max = 20)
        String zipCode,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        BigDecimal longitude,

        @NotNull(message = "Hourly rate is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Hourly rate must be positive")
        BigDecimal hourlyRate,

        @DecimalMin(value = "0.0", inclusive = false, message = "Daily rate must be positive")
        BigDecimal dailyRate,

        LocalTime openTime,

        LocalTime closeTime,

        @Size(max = 500)
        String description,

        @Size(max = 500)
        String amenities,

        /** Defaults to BOOKING_ONLY. Must be STAFFED_REALTIME or HYBRID for walk-up bookings / pay_on_exit to be allowed at this lot. */
        ParkingLot.ManagementMode managementMode,

        /**
         * Omit for an individual listing (ownership defaults to the acting
         * user). Set to list on behalf of a parking company — the acting
         * user must be an ORG_ADMIN of that organization, checked in
         * ParkingLotService, not here.
         */
        Long organizationId
) {
    public ParkingLotRequest {
        if (managementMode == null) managementMode = ParkingLot.ManagementMode.BOOKING_ONLY;
    }
}
