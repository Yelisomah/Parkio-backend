package com.example.parkio.controller;

import com.example.parkio.dto.request.StaffAssignmentRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.StaffAssignmentResponse;
import com.example.parkio.service.StaffAssignmentService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/staff-assignments")
@RequiredArgsConstructor
public class StaffAssignmentController {

    private final StaffAssignmentService staffAssignmentService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ApiResponse<StaffAssignmentResponse>> assign(
            @PathVariable Long organizationId, @Valid @RequestBody StaffAssignmentRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(staffAssignmentService.assign(organizationId, userId, request)));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> unassign(
            @PathVariable Long organizationId, @PathVariable Long assignmentId) {
        Long userId = securityUtils.getCurrentUserId();
        staffAssignmentService.unassign(organizationId, userId, assignmentId);
        return ResponseEntity.ok(ApiResponse.ok("Assignment removed"));
    }

    @GetMapping("/by-staff/{staffId}")
    public ResponseEntity<ApiResponse<List<StaffAssignmentResponse>>> getForStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.ok(staffAssignmentService.getForStaff(staffId)));
    }

    @GetMapping("/by-space/{spaceId}")
    public ResponseEntity<ApiResponse<List<StaffAssignmentResponse>>> getForSpace(@PathVariable Long spaceId) {
        return ResponseEntity.ok(ApiResponse.ok(staffAssignmentService.getForSpace(spaceId)));
    }
}
