package com.example.parkio.service;

import com.example.parkio.dto.request.FineDisputeRequest;
import com.example.parkio.dto.request.FineRequest;
import com.example.parkio.dto.response.BookingResponse;
import com.example.parkio.dto.response.FineResponse;
import com.example.parkio.dto.response.PlateStatusResponse;
import com.example.parkio.entity.*;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.BookingRepository;
import com.example.parkio.repository.FineRepository;
import com.example.parkio.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FineService {

    private static final EnumSet<Staff.StaffRole> FINE_ISSUER_ROLES =
            EnumSet.of(Staff.StaffRole.WARDEN, Staff.StaffRole.SUPERVISOR, Staff.StaffRole.ORG_ADMIN);

    private final FineRepository fineRepository;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final StaffService staffService;
    private final ParkingLotService parkingLotService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final AuditService auditService;

    /** Tied to the issuing staff member; notifies the vehicle owner if the plate is registered (tasks.md 11.3). */
    @Transactional
    public FineResponse issueFine(Long actingUserId, FineRequest request) {
        Staff staff = staffService.requireRole(request.organizationId(), actingUserId, FINE_ISSUER_ROLES);
        ParkingLot space = parkingLotService.findById(request.spaceId());

        String normalizedPlate = request.plateNumber().toUpperCase();

        Fine fine = Fine.builder()
                .plateNumber(normalizedPlate)
                .space(space)
                .issuedBy(staff)
                .reason(request.reason())
                .fineAmount(request.fineAmount())
                .build();
        Fine saved = fineRepository.save(fine);

        auditService.log(staff.getUser().getEmail(), AuditLog.AuditAction.FINE_ISSUED,
                "Fine", saved.getId(), "Fine issued to " + normalizedPlate + " at " + space.getName() + ": " + request.reason());

        // Best-effort — only notifies if the plate belongs to a registered vehicle.
        vehicleRepository.findByLicensePlate(normalizedPlate).ifPresent(vehicle ->
                notificationService.send(vehicle.getOwner().getId(), Notification.NotificationType.FINE_ISSUED,
                        "Fine Issued",
                        String.format("A fine of %s was issued to your vehicle (%s) at %s: %s",
                                request.fineAmount(), normalizedPlate, space.getName(), request.reason()),
                        saved.getId(), "Fine"));

        return FineResponse.from(saved);
    }

    /** Plate lookup — current payment status (tasks.md 11.2): active bookings + unpaid fines. */
    @Transactional(readOnly = true)
    public PlateStatusResponse getPlateStatus(String plateNumber) {
        String normalizedPlate = plateNumber.toUpperCase();

        List<BookingResponse> activeBookings = bookingRepository.findActiveByPlateNumber(normalizedPlate).stream()
                .map(BookingResponse::from)
                .toList();

        List<FineResponse> unpaidFines = fineRepository.findByPlateNumberAndPaidFalse(normalizedPlate).stream()
                .map(FineResponse::from)
                .toList();

        return new PlateStatusResponse(normalizedPlate, activeBookings, unpaidFines);
    }

    /**
     * Vehicle owner disputes a fine (task 11.4) — flags it for admin review
     * rather than resolving it automatically. "Routing to admin dashboard" is
     * implemented as a queryable flag (getDisputed()) rather than a separate
     * ticketing system — there's no dedicated admin-dashboard module in this
     * codebase to route into beyond that.
     */
    @Transactional
    public FineResponse disputeFine(Long fineId, Long actingUserId, FineDisputeRequest request) {
        Fine fine = findById(fineId);

        boolean ownsVehicle = vehicleRepository.findByLicensePlate(fine.getPlateNumber())
                .map(v -> v.getOwner().getId().equals(actingUserId))
                .orElse(false);
        if (!ownsVehicle) {
            throw ParkioException.forbidden("Only the registered owner of this plate can dispute this fine");
        }

        fine.setDisputed(true);
        fine.setDisputeReason(request.reason());
        Fine saved = fineRepository.save(fine);

        auditService.log(userService.findById(actingUserId).getEmail(), AuditLog.AuditAction.FINE_DISPUTED,
                "Fine", saved.getId(), "Disputed: " + request.reason());

        return FineResponse.from(saved);
    }

    @Transactional
    public FineResponse markPaid(Long fineId) {
        Fine fine = findById(fineId);
        fine.setPaid(true);
        Fine saved = fineRepository.save(fine);

        auditService.log("admin", AuditLog.AuditAction.FINE_PAID, "Fine", saved.getId(),
                "Marked paid: " + fine.getPlateNumber() + " " + fine.getFineAmount());

        return FineResponse.from(saved);
    }

    /** Compliance upholds a dispute — voids the fine entirely rather than collecting it. */
    @Transactional
    public FineResponse voidFine(Long fineId, String reviewerNote) {
        Fine fine = findById(fineId);
        fine.setPaid(false);
        fine.setDisputeReason((fine.getDisputeReason() != null ? fine.getDisputeReason() + " | " : "")
                + "VOIDED by compliance: " + reviewerNote);
        Fine saved = fineRepository.save(fine);

        auditService.log("compliance", AuditLog.AuditAction.FINE_DISPUTED,
                "Fine", saved.getId(), "Dispute upheld, fine voided: " + reviewerNote);

        return FineResponse.from(saved);
    }

    /** Admin dashboard: disputed fines awaiting review (task 11.4). */
    @Transactional(readOnly = true)
    public List<FineResponse> getDisputed() {
        return fineRepository.findByDisputedTrueOrderByIssuedAtDesc().stream()
                .map(FineResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Fine findById(Long id) {
        return fineRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Fine not found: " + id));
    }
}
