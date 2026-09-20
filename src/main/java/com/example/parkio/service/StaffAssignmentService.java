package com.example.parkio.service;

import com.example.parkio.dto.request.StaffAssignmentRequest;
import com.example.parkio.dto.response.StaffAssignmentResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.ParkingLot;
import com.example.parkio.entity.Staff;
import com.example.parkio.entity.StaffAssignment;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.StaffAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffAssignmentService {

    private final StaffAssignmentRepository staffAssignmentRepository;
    private final StaffService staffService;
    private final ParkingLotService parkingLotService;
    private final AuditService auditService;

    @Transactional
    public StaffAssignmentResponse assign(Long organizationId, Long actingUserId, StaffAssignmentRequest request) {
        staffService.requireRole(organizationId, actingUserId, EnumSet.of(Staff.StaffRole.ORG_ADMIN, Staff.StaffRole.SUPERVISOR));

        Staff staff = staffService.findByIdInOrg(organizationId, request.staffId());
        ParkingLot space = parkingLotService.findById(request.spaceId());

        // Closes a gap flagged since Phase B: without this, staff could be
        // assigned to a space owned by a completely different organization.
        if (space.getOrganization() == null || !space.getOrganization().getId().equals(organizationId)) {
            throw ParkioException.badRequest("This organization does not own that space");
        }

        if (staffAssignmentRepository.existsByStaffIdAndSpaceIdAndActiveTrue(staff.getId(), space.getId())) {
            throw ParkioException.conflict("Staff member is already assigned to this space");
        }

        StaffAssignment assignment = StaffAssignment.builder()
                .staff(staff)
                .space(space)
                .assignedAt(LocalDateTime.now())
                .build();
        StaffAssignment saved = staffAssignmentRepository.save(assignment);

        auditService.log(staff.getUser().getEmail(), AuditLog.AuditAction.STAFF_ASSIGNED_TO_SPACE,
                "StaffAssignment", saved.getId(), staff.getUser().getEmail() + " assigned to space " + space.getName());

        return StaffAssignmentResponse.from(saved);
    }

    @Transactional
    public void unassign(Long organizationId, Long actingUserId, Long assignmentId) {
        staffService.requireRole(organizationId, actingUserId, EnumSet.of(Staff.StaffRole.ORG_ADMIN, Staff.StaffRole.SUPERVISOR));

        StaffAssignment assignment = staffAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ParkioException.notFound("Assignment not found"));
        if (!assignment.getStaff().getOrganization().getId().equals(organizationId)) {
            throw ParkioException.notFound("Assignment not found in this organization");
        }

        assignment.setActive(false);
        staffAssignmentRepository.save(assignment);

        auditService.log(assignment.getStaff().getUser().getEmail(), AuditLog.AuditAction.STAFF_UNASSIGNED_FROM_SPACE,
                "StaffAssignment", assignment.getId(), "Unassigned from space " + assignment.getSpace().getName());
    }

    @Transactional(readOnly = true)
    public List<StaffAssignmentResponse> getForStaff(Long staffId) {
        return staffAssignmentRepository.findByStaffIdAndActiveTrue(staffId).stream()
                .map(StaffAssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffAssignmentResponse> getForSpace(Long spaceId) {
        return staffAssignmentRepository.findBySpaceIdAndActiveTrue(spaceId).stream()
                .map(StaffAssignmentResponse::from)
                .toList();
    }

    /** True if the given staff member is currently assigned to the given space — used by ShiftService to validate shift scheduling. */
    @Transactional(readOnly = true)
    public boolean isAssigned(Long staffId, Long spaceId) {
        return staffAssignmentRepository.existsByStaffIdAndSpaceIdAndActiveTrue(staffId, spaceId);
    }
}
