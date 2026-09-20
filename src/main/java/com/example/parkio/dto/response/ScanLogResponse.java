package com.example.parkio.dto.response;

import com.example.parkio.entity.ScanLog;

import java.time.LocalDateTime;

public record ScanLogResponse(
        Long id,
        Long bookingId,
        String bookingReference,
        String rawQrCode,
        ScanLog.ScanAction action,
        LocalDateTime scanTime,
        boolean offlineSynced,
        ScanLog.ReconciliationStatus reconciliationStatus,
        String reconciliationNote
) {
    public static ScanLogResponse from(ScanLog s) {
        return new ScanLogResponse(
                s.getId(),
                s.getBooking() != null ? s.getBooking().getId() : null,
                s.getBooking() != null ? s.getBooking().getBookingReference() : null,
                s.getRawQrCode(),
                s.getAction(),
                s.getScanTime(),
                s.isOfflineSynced(),
                s.getReconciliationStatus(),
                s.getReconciliationNote()
        );
    }
}
