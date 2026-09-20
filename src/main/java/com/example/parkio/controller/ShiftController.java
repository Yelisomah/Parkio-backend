package com.example.parkio.controller;

import com.example.parkio.dto.request.ShiftRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.ShiftResponse;
import com.example.parkio.service.ShiftService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ApiResponse<ShiftResponse>> schedule(
            @PathVariable Long organizationId, @Valid @RequestBody ShiftRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(shiftService.schedule(organizationId, userId, request)));
    }

    /** Staff member checks themselves in to their own shift. */
    @PatchMapping("/{shiftId}/check-in")
    public ResponseEntity<ApiResponse<ShiftResponse>> checkIn(
            @PathVariable Long organizationId, @PathVariable Long shiftId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Checked in", shiftService.checkIn(shiftId, userId)));
    }

    @PatchMapping("/{shiftId}/check-out")
    public ResponseEntity<ApiResponse<ShiftResponse>> checkOut(
            @PathVariable Long organizationId, @PathVariable Long shiftId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Checked out", shiftService.checkOut(shiftId, userId)));
    }

    @GetMapping("/by-staff/{staffId}")
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> getForStaff(
            @PathVariable Long organizationId, @PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getForStaff(staffId)));
    }
}
