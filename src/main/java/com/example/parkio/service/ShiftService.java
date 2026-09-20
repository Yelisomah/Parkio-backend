package com.example.parkio.service;

import com.example.parkio.dto.request.ShiftRequest;
import com.example.parkio.dto.response.ShiftResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.ParkingLot;
import com.example.parkio.entity.Shift;
import com.example.parkio.entity.Staff;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final StaffService staffService;
    private final StaffAssignmentService staffAssignmentService;
    private final ParkingLotService parkingLotService;
    private final AuditService auditService;

    /** How early a staff member is allowed to check in before the scheduled start. */
    private static final long CHECK_IN_EARLY_GRACE_MINUTES = 15;

    @Transactional
    public ShiftResponse schedule(Long organizationId, Long actingUserId, ShiftRequest request) {
        staffService.requireRole(organizationId, actingUserId,
                EnumSet.of(Staff.StaffRole.ORG_ADMIN, Staff.StaffRole.SUPERVISOR));

        Staff staff = staffService.findByIdInOrg(organizationId, request.staffId());
        ParkingLot space = parkingLotService.findById(request.spaceId());

        if (!staffAssignmentService.isAssigned(staff.getId(), space.getId())) {
            throw ParkioException.badRequest("Staff member must be assigned to this space before a shift can be scheduled");
        }

        Shift shift = Shift.builder()
                .staff(staff)
                .space(space)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();
        Shift saved = shiftRepository.save(shift);

        auditService.log(staff.getUser().getEmail(), AuditLog.AuditAction.SHIFT_SCHEDULED,
                "Shift", saved.getId(), "Shift scheduled at " + space.getName() + " from " + request.startTime() + " to " + request.endTime());

        return ShiftResponse.from(saved);
    }

    /** Staff member checks themselves in — must be within the grace window before/during the shift. */
    @Transactional
    public ShiftResponse checkIn(Long shiftId, Long actingUserId) {
        Shift shift = findById(shiftId);
        assertOwnShift(shift, actingUserId);

        if (shift.getAttendanceStatus() != Shift.AttendanceStatus.SCHEDULED) {
            throw ParkioException.badRequest("Cannot check in — shift is " + shift.getAttendanceStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestAllowed = shift.getStartTime().minus(CHECK_IN_EARLY_GRACE_MINUTES, ChronoUnit.MINUTES);
        if (now.isBefore(earliestAllowed)) {
            throw ParkioException.badRequest("Too early to check in — shift starts at " + shift.getStartTime());
        }
        if (now.isAfter(shift.getEndTime())) {
            throw ParkioException.badRequest("Shift has already ended");
        }

        shift.setCheckInTime(now);
        shift.setAttendanceStatus(Shift.AttendanceStatus.CHECKED_IN);
        Shift saved = shiftRepository.save(shift);

        auditService.log(shift.getStaff().getUser().getEmail(), AuditLog.AuditAction.SHIFT_CHECKED_IN,
                "Shift", saved.getId(), "Checked in at " + shift.getSpace().getName());

        return ShiftResponse.from(saved);
    }

    @Transactional
    public ShiftResponse checkOut(Long shiftId, Long actingUserId) {
        Shift shift = findById(shiftId);
        assertOwnShift(shift, actingUserId);

        if (shift.getAttendanceStatus() != Shift.AttendanceStatus.CHECKED_IN) {
            throw ParkioException.badRequest("Cannot check out — shift is " + shift.getAttendanceStatus());
        }

        shift.setCheckOutTime(LocalDateTime.now());
        shift.setAttendanceStatus(Shift.AttendanceStatus.COMPLETED);
        Shift saved = shiftRepository.save(shift);

        auditService.log(shift.getStaff().getUser().getEmail(), AuditLog.AuditAction.SHIFT_CHECKED_OUT,
                "Shift", saved.getId(), "Checked out of " + shift.getSpace().getName());

        return ShiftResponse.from(saved);
    }

    /** Called by StaffSchedulerService's no-show sweep. */
    @Transactional
    public void markNoShow(Shift shift) {
        shift.setAttendanceStatus(Shift.AttendanceStatus.NO_SHOW);
        shiftRepository.save(shift);

        auditService.log(shift.getStaff().getUser().getEmail(), AuditLog.AuditAction.SHIFT_NO_SHOW,
                "Shift", shift.getId(), "Auto-marked NO_SHOW — no check-in within grace window at " + shift.getSpace().getName());
    }

    @Transactional(readOnly = true)
    public List<Shift> findOverdueForNoShow(LocalDateTime cutoff) {
        return shiftRepository.findByAttendanceStatusAndStartTimeBefore(Shift.AttendanceStatus.SCHEDULED, cutoff);
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> getForStaff(Long staffId) {
        return shiftRepository.findByStaffIdOrderByStartTimeDesc(staffId).stream()
                .map(ShiftResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Shift findById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Shift not found"));
    }

    private void assertOwnShift(Shift shift, Long actingUserId) {
        if (!shift.getStaff().getUser().getId().equals(actingUserId)) {
            throw ParkioException.forbidden("Not your shift");
        }
    }
}
