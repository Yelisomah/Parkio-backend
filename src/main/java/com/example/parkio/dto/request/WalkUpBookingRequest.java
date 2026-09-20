package com.example.parkio.dto.request;

import com.example.parkio.entity.Booking;
import com.example.parkio.entity.Vehicle;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Warden-initiated walk-up booking (tasks.md task 6) — the driver isn't
 * necessarily a registered app user, so we take a plate + phone instead of
 * a userId/vehicleId. See UserService.findOrCreateGuestByPhone /
 * VehicleService.findOrCreateByPlate for how those get resolved to real
 * records under the hood.
 */
public record WalkUpBookingRequest(
        @NotNull Long organizationId,
        @NotNull Long spaceId,
        @NotBlank String vehiclePlate,
        Vehicle.VehicleType vehicleType,
        @NotBlank String driverPhone,
        String driverName,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        /** Defaults to PREPAID; PAY_ON_EXIT is only legal at STAFFED_REALTIME/HYBRID spaces — enforced server-side regardless of what's sent here. */
        Booking.PaymentTiming paymentTiming
) {
    public WalkUpBookingRequest {
        if (paymentTiming == null) paymentTiming = Booking.PaymentTiming.PREPAID;
    }

    @AssertTrue(message = "endTime must be after startTime")
    public boolean isEndAfterStart() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
