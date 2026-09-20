package com.example.parkio.controller;

import com.example.parkio.dto.request.ParkingLotRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.NearbyLotResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.dto.response.ParkingLotResponse;
import com.example.parkio.service.ParkingLotService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parking-lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService parkingLotService;
    private final SecurityUtils securityUtils;

    /** Public — list APPROVED, active lots (paginated) */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ParkingLotResponse>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getAll(pageable)));
    }

    /** Public — get single lot (any status — an owner checking their own PENDING listing hits this too) */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ParkingLotResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getById(id)));
    }

    /** Public — keyword search (APPROVED only) */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ParkingLotResponse>>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.search(keyword, pageable)));
    }

    /** Public — filter by city (APPROVED only) */
    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<List<ParkingLotResponse>>> getByCity(
            @PathVariable String city) {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getByCity(city)));
    }

    /** Public — geospatial radius search, e.g. /nearby?lat=5.56&lng=-0.20&radiusKm=2 (Postgres+PostGIS only, see repository) */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyLotResponse>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radiusKm) {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getNearby(lat, lng, radiusKm)));
    }

    /** Authenticated — everything you own individually or via a company you're staff of, any approval status. */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ParkingLotResponse>>> getMine() {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getMine(securityUtils.getCurrentUserId())));
    }

    // ── Listing management — owner (individual/company) or internal staff ────

    /**
     * Any authenticated user can list a space: individually (default), on
     * behalf of a company (pass organizationId, must be that org's
     * ORG_ADMIN), or platform-operated (internal staff, no organizationId).
     * Always starts PENDING_APPROVAL — same workflow for everyone.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ParkingLotResponse>> create(@Valid @RequestBody ParkingLotRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        boolean internal = securityUtils.isInternalStaff();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(parkingLotService.create(userId, internal, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ParkingLotResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ParkingLotRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        boolean internal = securityUtils.isInternalStaff();
        return ResponseEntity.ok(ApiResponse.ok("Lot updated", parkingLotService.update(id, userId, internal, request)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        parkingLotService.deactivate(id, userId, securityUtils.isInternalStaff());
        return ResponseEntity.ok(ApiResponse.ok("Parking lot deactivated"));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        parkingLotService.activate(id, userId, securityUtils.isInternalStaff());
        return ResponseEntity.ok(ApiResponse.ok("Parking lot activated"));
    }

    // ── Internal team: listing approval workflow (ROLE_ADMIN / ROLE_OPS) ──────

    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPS')")
    public ResponseEntity<ApiResponse<List<ParkingLotResponse>>> getPendingApproval() {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getPendingApproval()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPS')")
    public ResponseEntity<ApiResponse<ParkingLotResponse>> approve(@PathVariable Long id) {
        String reviewer = securityUtils.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.approve(id, reviewer)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPS')")
    public ResponseEntity<ApiResponse<ParkingLotResponse>> reject(
            @PathVariable Long id, @RequestParam String reason) {
        String reviewer = securityUtils.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.reject(id, reviewer, reason)));
    }
}
