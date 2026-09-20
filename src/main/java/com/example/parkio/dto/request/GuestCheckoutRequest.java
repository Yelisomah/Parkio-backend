package com.example.parkio.dto.request;

import com.example.parkio.entity.Vehicle;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Confirms a QR-signage guest checkout (Phase 2, tasks.md 13.1/13.2) — the
 * OTP proves phone ownership; there's no JWT/account login involved before
 * this call. See GuestCheckoutService for what happens after: a real
 * session token IS issued once the OTP checks out, so the rest of the app
 * (payment, viewing the booking, etc.) works normally from that point on.
 */
public record GuestCheckoutRequest(
        @NotBlank String driverPhone,
        @NotBlank String otpCode,
        String driverName,
        @NotBlank String vehiclePlate,
        Vehicle.VehicleType vehicleType,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String promoCode
) {
    @AssertTrue(message = "endTime must be after startTime")
    public boolean isEndAfterStart() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
