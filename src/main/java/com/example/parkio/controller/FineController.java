package com.example.parkio.controller;

import com.example.parkio.dto.request.FineDisputeRequest;
import com.example.parkio.dto.request.FineRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.FineResponse;
import com.example.parkio.dto.response.PlateStatusResponse;
import com.example.parkio.service.FineService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;
    private final SecurityUtils securityUtils;

    /** Staff (warden/supervisor/org_admin) issues a fine (task 11.3). */
    @PostMapping
    public ResponseEntity<ApiResponse<FineResponse>> issue(@Valid @RequestBody FineRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(fineService.issueFine(userId, request)));
    }

    /** Plate lookup (task 11.2) — any authenticated user; consider restricting to staff/admin if this proves too permissive. */
    @GetMapping("/plate/{plateNumber}")
    public ResponseEntity<ApiResponse<PlateStatusResponse>> getPlateStatus(@PathVariable String plateNumber) {
        return ResponseEntity.ok(ApiResponse.ok(fineService.getPlateStatus(plateNumber)));
    }

    /** The vehicle's registered owner disputes a fine (task 11.4). */
    @PostMapping("/{id}/dispute")
    public ResponseEntity<ApiResponse<FineResponse>> dispute(
            @PathVariable Long id, @Valid @RequestBody FineDisputeRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(fineService.disputeFine(id, userId, request)));
    }

    /** Compliance/admin: fines currently disputed, awaiting review. */
    @GetMapping("/disputed")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPLIANCE')")
    public ResponseEntity<ApiResponse<List<FineResponse>>> getDisputed() {
        return ResponseEntity.ok(ApiResponse.ok(fineService.getDisputed()));
    }

    /** Compliance/admin: resolve a dispute by marking the fine paid (i.e. the dispute didn't hold up). */
    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPLIANCE')")
    public ResponseEntity<ApiResponse<FineResponse>> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(fineService.markPaid(id)));
    }

    /** Compliance/admin: uphold a dispute — void the fine rather than collecting it. */
    @PatchMapping("/{id}/void")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPLIANCE')")
    public ResponseEntity<ApiResponse<FineResponse>> voidFine(
            @PathVariable Long id, @RequestParam String reviewerNote) {
        return ResponseEntity.ok(ApiResponse.ok(fineService.voidFine(id, reviewerNote)));
    }
}
