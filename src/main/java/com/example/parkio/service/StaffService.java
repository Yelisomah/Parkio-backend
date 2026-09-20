package com.example.parkio.service;

import com.example.parkio.dto.request.StaffRequest;
import com.example.parkio.dto.request.StaffRoleUpdateRequest;
import com.example.parkio.dto.response.StaffResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.Organization;
import com.example.parkio.entity.Staff;
import com.example.parkio.entity.User;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final AuditService auditService;

    /** Roles allowed to manage staff (add/remove/change role) within an organization. */
    private static final Set<Staff.StaffRole> STAFF_MANAGERS = EnumSet.of(Staff.StaffRole.ORG_ADMIN);

    @Transactional
    public StaffResponse addStaff(Long organizationId, Long actingUserId, StaffRequest request) {
        Organization org = organizationService.findById(organizationId);
        requireRole(organizationId, actingUserId, STAFF_MANAGERS);

        if (staffRepository.existsByOrganizationIdAndUserIdAndActiveTrue(organizationId, request.userId())) {
            throw ParkioException.conflict("This user is already staff at this organization");
        }

        User user = userService.findById(request.userId());

        Staff reportsTo = null;
        if (request.reportsToStaffId() != null) {
            reportsTo = staffRepository.findById(request.reportsToStaffId())
                    .orElseThrow(() -> ParkioException.notFound("reportsToStaffId not found"));
            // tasks.md 3.3: "reject cross-organization reporting lines"
            if (!reportsTo.getOrganization().getId().equals(organizationId)) {
                throw ParkioException.badRequest("reportsTo must belong to the same organization");
            }
        }

        Staff staff = Staff.builder()
                .organization(org)
                .user(user)
                .role(request.role())
                .reportsTo(reportsTo)
                .build();
        Staff saved = staffRepository.save(staff);

        auditService.log(user.getEmail(), AuditLog.AuditAction.STAFF_ADDED,
                "Staff", saved.getId(), user.getEmail() + " added to org " + organizationId + " as " + request.role());

        return StaffResponse.from(saved);
    }

    @Transactional
    public StaffResponse updateRole(Long organizationId, Long staffId, Long actingUserId, StaffRoleUpdateRequest request) {
        requireRole(organizationId, actingUserId, STAFF_MANAGERS);
        Staff staff = findByIdInOrg(organizationId, staffId);

        staff.setRole(request.role());
        Staff saved = staffRepository.save(staff);

        auditService.log(staff.getUser().getEmail(), AuditLog.AuditAction.STAFF_ROLE_CHANGED,
                "Staff", saved.getId(), "Role changed to " + request.role());

        return StaffResponse.from(saved);
    }

    @Transactional
    public void removeStaff(Long organizationId, Long staffId, Long actingUserId) {
        requireRole(organizationId, actingUserId, STAFF_MANAGERS);
        Staff staff = findByIdInOrg(organizationId, staffId);

        // Anyone reporting directly to this staff member is orphaned — clear the
        // link rather than leaving a dangling reference to a now-inactive staff row.
        List<Staff> directReports = staffRepository.findByReportsToId(staff.getId());
        directReports.forEach(r -> r.setReportsTo(null));
        staffRepository.saveAll(directReports);

        staff.setActive(false);
        staffRepository.save(staff);

        auditService.log(staff.getUser().getEmail(), AuditLog.AuditAction.STAFF_REMOVED,
                "Staff", staff.getId(), "Removed from org " + organizationId);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> listByOrganization(Long organizationId, Long actingUserId) {
        // Any active staff member (not just managers) can see their own org's roster.
        requireRole(organizationId, actingUserId, EnumSet.allOf(Staff.StaffRole.class));
        return staffRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .map(StaffResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Staff findByIdInOrg(Long organizationId, Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> ParkioException.notFound("Staff not found"));
        if (!staff.getOrganization().getId().equals(organizationId)) {
            throw ParkioException.notFound("Staff not found in this organization");
        }
        return staff;
    }

    /**
     * Central authorization check for the whole module: does the acting user
     * hold one of the allowed roles as ACTIVE staff of this organization?
     * Business-level, resource-scoped authorization — deliberately NOT modeled
     * as Spring Security authorities, since org_admin/supervisor/warden are
     * per-organization, not global like ROLE_USER/ROLE_ADMIN.
     */
    @Transactional(readOnly = true)
    public Staff requireRole(Long organizationId, Long userId, Set<Staff.StaffRole> allowedRoles) {
        Staff staff = staffRepository.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId)
                .orElseThrow(() -> ParkioException.forbidden("Not an active staff member of this organization"));
        if (!allowedRoles.contains(staff.getRole())) {
            throw ParkioException.forbidden("Requires one of " + allowedRoles + ", but caller is " + staff.getRole());
        }
        return staff;
    }
}
