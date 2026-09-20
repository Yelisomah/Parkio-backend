package com.example.parkio.controller;

import com.example.parkio.dto.request.StaffRequest;
import com.example.parkio.dto.request.StaffRoleUpdateRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.StaffResponse;
import com.example.parkio.service.StaffService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Authorization here is business-level (StaffService.requireRole), not
 * @PreAuthorize authorities — org_admin/supervisor/warden are scoped per
 * organization, not global platform roles. See StaffService for the
 * rationale.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> addStaff(
            @PathVariable Long organizationId, @Valid @RequestBody StaffRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(staffService.addStaff(organizationId, userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffResponse>>> list(@PathVariable Long organizationId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(staffService.listByOrganization(organizationId, userId)));
    }

    @PatchMapping("/{staffId}/role")
    public ResponseEntity<ApiResponse<StaffResponse>> updateRole(
            @PathVariable Long organizationId, @PathVariable Long staffId,
            @Valid @RequestBody StaffRoleUpdateRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(staffService.updateRole(organizationId, staffId, userId, request)));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long organizationId, @PathVariable Long staffId) {
        Long userId = securityUtils.getCurrentUserId();
        staffService.removeStaff(organizationId, staffId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Staff member removed"));
    }
}
