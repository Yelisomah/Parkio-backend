package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A staff QR scan against a booking — check-in or check-out. Recorded for
 * both online scans (immediate) and offline scans (synced later, then
 * reconciled against authoritative booking state — see design.md's "Offline
 * QR scan and reconciliation" flow and tasks.md 8.3).
 */
@Entity
@Table(name = "scan_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable — an offline scan for a QR code that doesn't match ANY booking (typo, corrupted cache, fraud attempt) still gets logged as a MISMATCH for admin review rather than silently dropped; see rawQrCode in that case. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    /** Always set; redundant with booking.qrCode when booking is resolved, but the only record of what was scanned when it ISN'T. */
    @Column(name = "raw_qr_code", nullable = false, length = 40)
    private String rawQrCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by_staff_id", nullable = false)
    private Staff scannedByStaff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanAction action;

    /** When the scan actually happened (client-reported for offline scans, server time for online ones). */
    @Column(nullable = false)
    private LocalDateTime scanTime;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    @Builder.Default
    private boolean offlineSynced = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.OK;

    @Column(length = 500)
    private String reconciliationNote;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ScanAction {
        CHECK_IN, CHECK_OUT
    }

    public enum ReconciliationStatus {
        /** Applied cleanly — either an online scan, or an offline scan whose recorded action was still legal on reconnect. */
        OK,
        /** Offline scan whose recorded action was NO LONGER legal by the time it synced (e.g. booking cancelled meanwhile) — flagged for admin review, not applied. */
        MISMATCH
    }
}
