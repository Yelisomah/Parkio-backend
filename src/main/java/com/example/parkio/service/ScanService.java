package com.example.parkio.service;

import com.example.parkio.dto.request.OfflineScanBatchRequest;
import com.example.parkio.dto.request.QrScanRequest;
import com.example.parkio.dto.response.OfflineSyncEntryResponse;
import com.example.parkio.dto.response.ScanLogResponse;
import com.example.parkio.entity.*;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.BookingRepository;
import com.example.parkio.repository.ScanLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Staff QR scan handling (tasks.md task 8) — online validation, the offline
 * validation ruleset staff apps cache, and reconciliation of scans recorded
 * offline once the device reconnects. See design.md's "Offline QR scan and
 * reconciliation" flow for the intended shape of all three.
 */
@Service
@RequiredArgsConstructor
public class ScanService {

    private static final EnumSet<Staff.StaffRole> SCAN_ROLES =
            EnumSet.of(Staff.StaffRole.WARDEN, Staff.StaffRole.SUPERVISOR, Staff.StaffRole.ORG_ADMIN);

    private final BookingRepository bookingRepository;
    private final ScanLogRepository scanLogRepository;
    private final BookingService bookingService;
    private final StaffService staffService;
    private final StaffAssignmentService staffAssignmentService;
    private final AuditService auditService;

    /**
     * Online scan (task 8.1) — the common case, immediate and authoritative.
     * Actually performs the check-in/check-out (via BookingService's
     * state-machine-guarded staff path), then logs the scan.
     */
    @Transactional
    public ScanLogResponse scanOnline(Long organizationId, Long actingUserId, QrScanRequest request) {
        Staff staff = staffService.requireRole(organizationId, actingUserId, SCAN_ROLES);

        Optional<Booking> maybeBooking = bookingRepository.findByQrCode(request.qrCode());
        if (maybeBooking.isEmpty()) {
            ScanLog rejected = persistMismatch(staff, request.qrCode(), request.action(),
                    LocalDateTime.now(), request.latitude(), request.longitude(), false,
                    "No booking matches this QR code");
            throw ParkioException.notFound("Unrecognized booking QR code (logged as scan #" + rejected.getId() + " for review)");
        }
        Booking booking = maybeBooking.get();
        assertStaffAssignedToBookingSpace(staff, booking);

        Booking updated = switch (request.action()) {
            case CHECK_IN -> bookingService.staffCheckIn(booking.getId(), staff);
            case CHECK_OUT -> bookingService.staffCheckOut(booking.getId(), staff);
        };

        ScanLog log = ScanLog.builder()
                .booking(updated)
                .rawQrCode(request.qrCode())
                .scannedByStaff(staff)
                .action(request.action())
                .scanTime(LocalDateTime.now())
                .latitude(toBigDecimal(request.latitude()))
                .longitude(toBigDecimal(request.longitude()))
                .offlineSynced(false)
                .reconciliationStatus(ScanLog.ReconciliationStatus.OK)
                .build();
        ScanLog saved = scanLogRepository.save(log);

        auditService.log(staff.getUser().getEmail(), AuditLog.AuditAction.SCAN_RECORDED,
                "ScanLog", saved.getId(), request.action() + " scan for booking " + updated.getBookingReference());

        return ScanLogResponse.from(saved);
    }

    /**
     * Offline validation ruleset (task 8.2) — everything a staff app needs to
     * validate a scan locally without connectivity: which bookings are
     * currently scannable at this space, and which action is legal for each.
     * A short window (2h lookback / 24h lookahead) keeps the cache small and
     * short-lived, per design.md ("short-lived local cache").
     */
    @Transactional(readOnly = true)
    public List<OfflineSyncEntryResponse> getOfflineSyncRuleset(Long organizationId, Long actingUserId, Long spaceId) {
        Staff staff = staffService.requireRole(organizationId, actingUserId, EnumSet.allOf(Staff.StaffRole.class));
        if (!staffAssignmentService.isAssigned(staff.getId(), spaceId)) {
            throw ParkioException.forbidden("Not assigned to this space");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Booking> scannable = bookingRepository.findScannableAtLotInWindow(
                spaceId, now.minusHours(2), now.plusHours(24));

        return scannable.stream()
                .map(b -> OfflineSyncEntryResponse.from(b,
                        b.getStatus() == Booking.BookingStatus.CONFIRMED
                                ? ScanLog.ScanAction.CHECK_IN
                                : ScanLog.ScanAction.CHECK_OUT))
                .toList();
    }

    /**
     * Offline reconciliation (task 8.3) — each entry was already validated
     * AGAINST THE CACHE on the device while offline; now we re-validate
     * against current authoritative state. If the action recorded offline is
     * STILL legal now, apply it. If not (e.g. the booking was cancelled by
     * someone else during the offline window — design.md's own example),
     * DON'T apply it — log it as a MISMATCH for admin review instead.
     */
    @Transactional
    public List<ScanLogResponse> reconcileOfflineScans(Long organizationId, Long actingUserId, OfflineScanBatchRequest request) {
        Staff staff = staffService.requireRole(organizationId, actingUserId, SCAN_ROLES);

        return request.scans().stream()
                .map(entry -> reconcileOne(staff, entry))
                .map(ScanLogResponse::from)
                .toList();
    }

    private ScanLog reconcileOne(Staff staff, OfflineScanBatchRequest.Entry entry) {
        Optional<Booking> maybeBooking = bookingRepository.findByQrCode(entry.qrCode());
        if (maybeBooking.isEmpty()) {
            return persistMismatch(staff, entry.qrCode(), entry.action(), entry.scanTime(),
                    entry.latitude(), entry.longitude(), true, "No booking matches this QR code");
        }

        Booking booking = maybeBooking.get();

        boolean staffStillAssigned = staffAssignmentService.isAssigned(
                staff.getId(), booking.getSpot().getParkingLot().getId());
        if (!staffStillAssigned) {
            return persistMismatchForBooking(staff, booking, entry, "Staff no longer assigned to this booking's space");
        }

        // The recorded offline action is only safe to apply now if it's STILL
        // a legal transition from the booking's CURRENT status — e.g. if it
        // was cancelled server-side during the offline window, this is false.
        boolean stillLegal = switch (entry.action()) {
            case CHECK_IN -> booking.getStatus() == Booking.BookingStatus.CONFIRMED;
            case CHECK_OUT -> booking.getStatus() == Booking.BookingStatus.ACTIVE;
        };

        if (!stillLegal) {
            return persistMismatchForBooking(staff, booking, entry,
                    "Booking status is now " + booking.getStatus() + " — the offline " + entry.action()
                            + " recorded at " + entry.scanTime() + " is no longer valid");
        }

        Booking updated = entry.action() == ScanLog.ScanAction.CHECK_IN
                ? bookingService.staffCheckIn(booking.getId(), staff)
                : bookingService.staffCheckOut(booking.getId(), staff);

        ScanLog log = ScanLog.builder()
                .booking(updated)
                .rawQrCode(entry.qrCode())
                .scannedByStaff(staff)
                .action(entry.action())
                .scanTime(entry.scanTime())
                .latitude(toBigDecimal(entry.latitude()))
                .longitude(toBigDecimal(entry.longitude()))
                .offlineSynced(true)
                .reconciliationStatus(ScanLog.ReconciliationStatus.OK)
                .build();
        return scanLogRepository.save(log);
    }

    private ScanLog persistMismatchForBooking(Staff staff, Booking booking, OfflineScanBatchRequest.Entry entry, String note) {
        ScanLog log = ScanLog.builder()
                .booking(booking)
                .rawQrCode(entry.qrCode())
                .scannedByStaff(staff)
                .action(entry.action())
                .scanTime(entry.scanTime())
                .latitude(toBigDecimal(entry.latitude()))
                .longitude(toBigDecimal(entry.longitude()))
                .offlineSynced(true)
                .reconciliationStatus(ScanLog.ReconciliationStatus.MISMATCH)
                .reconciliationNote(note)
                .build();
        ScanLog saved = scanLogRepository.save(log);

        auditService.log(staff.getUser().getEmail(), AuditLog.AuditAction.SCAN_RECONCILIATION_MISMATCH,
                "ScanLog", saved.getId(), note);

        return saved;
    }

    private ScanLog persistMismatch(Staff staff, String rawQrCode, ScanLog.ScanAction action, LocalDateTime scanTime,
                                    Double latitude, Double longitude, boolean offlineSynced, String note) {
        ScanLog log = ScanLog.builder()
                .booking(null)
                .rawQrCode(rawQrCode)
                .scannedByStaff(staff)
                .action(action)
                .scanTime(scanTime)
                .latitude(toBigDecimal(latitude))
                .longitude(toBigDecimal(longitude))
                .offlineSynced(offlineSynced)
                .reconciliationStatus(ScanLog.ReconciliationStatus.MISMATCH)
                .reconciliationNote(note)
                .build();
        ScanLog saved = scanLogRepository.save(log);

        auditService.log(staff.getUser().getEmail(), AuditLog.AuditAction.SCAN_RECONCILIATION_MISMATCH,
                "ScanLog", saved.getId(), note);

        return saved;
    }

    private void assertStaffAssignedToBookingSpace(Staff staff, Booking booking) {
        Long spaceId = booking.getSpot().getParkingLot().getId();
        if (!staffAssignmentService.isAssigned(staff.getId(), spaceId)) {
            throw ParkioException.forbidden("Not assigned to this booking's space");
        }
    }

    private BigDecimal toBigDecimal(Double d) {
        return d != null ? BigDecimal.valueOf(d) : null;
    }
}
