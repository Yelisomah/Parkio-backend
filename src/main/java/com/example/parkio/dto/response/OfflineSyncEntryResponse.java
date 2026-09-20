package com.example.parkio.dto.response;

import com.example.parkio.entity.Booking;
import com.example.parkio.entity.ScanLog;

import java.time.LocalDateTime;

/**
 * One entry in the offline validation ruleset a staff app caches locally
 * (task 8.2) so it can validate a scan without connectivity. "expectedAction"
 * tells the app which action is legal right now, given the booking's current
 * (last-known) status — CHECK_IN for CONFIRMED bookings, CHECK_OUT for ACTIVE
 * ones. Bookings in any other status aren't included at all — they aren't a
 * legal scan target from the field.
 */
public record OfflineSyncEntryResponse(
        Long bookingId,
        String qrCode,
        String driverName,
        String vehiclePlate,
        String spotNumber,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        ScanLog.ScanAction expectedAction
) {
    public static OfflineSyncEntryResponse from(Booking b, ScanLog.ScanAction expectedAction) {
        return new OfflineSyncEntryResponse(
                b.getId(),
                b.getQrCode(),
                b.getUser().getFirstName() + " " + b.getUser().getLastName(),
                b.getVehicle().getLicensePlate(),
                b.getSpot().getSpotNumber(),
                b.getStartTime(),
                b.getEndTime(),
                expectedAction
        );
    }
}
