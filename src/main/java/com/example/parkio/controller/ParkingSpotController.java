package com.example.parkio.controller;

import com.example.parkio.dto.request.BulkSpotRequest;
import com.example.parkio.dto.request.ParkingSpotRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.ParkingSpotResponse;
import com.example.parkio.entity.ParkingSpot;
import com.example.parkio.service.ParkingSpotService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ParkingSpotController {

    private final ParkingSpotService parkingSpotService;
    private final SecurityUtils securityUtils;

    /** Public — all active spots in a lot */
    @GetMapping("/parking-lots/{lotId}/spots")
    public ResponseEntity<ApiResponse<List<ParkingSpotResponse>>> getByLot(
            @PathVariable Long lotId) {
        return ResponseEntity.ok(ApiResponse.ok(parkingSpotService.getByLot(lotId)));
    }

    /** Public — available spots for a time window */
    @GetMapping("/parking-lots/{lotId}/spots/available")
    public ResponseEntity<ApiResponse<List<ParkingSpotResponse>>> getAvailable(
            @PathVariable Long lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(ApiResponse.ok(
                parkingSpotService.getAvailable(lotId, startTime, endTime)));
    }

    /** Public — single spot detail */
    @GetMapping("/parking-spots/{id}")
    public ResponseEntity<ApiResponse<ParkingSpotResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(parkingSpotService.getById(id)));
    }

    // ── Spot management — the owning lot's owner/company/internal staff ──────

    @PostMapping("/parking-lots/{lotId}/spots")
    public ResponseEntity<ApiResponse<ParkingSpotResponse>> create(
            @PathVariable Long lotId,
            @Valid @RequestBody ParkingSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(parkingSpotService.create(
                        lotId, securityUtils.getCurrentUserId(), securityUtils.isInternalStaff(), request)));
    }

    /** Create many spots in one call, e.g. {prefix:"A-", count:20, type:"STANDARD"} → A-01..A-20 */
    @PostMapping("/parking-lots/{lotId}/spots/bulk")
    public ResponseEntity<ApiResponse<List<ParkingSpotResponse>>> createBulk(
            @PathVariable Long lotId,
            @Valid @RequestBody BulkSpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(parkingSpotService.createBulk(
                        lotId, securityUtils.getCurrentUserId(), securityUtils.isInternalStaff(), request)));
    }

    @PutMapping("/parking-spots/{id}")
    public ResponseEntity<ApiResponse<ParkingSpotResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ParkingSpotRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Spot updated", parkingSpotService.update(
                id, securityUtils.getCurrentUserId(), securityUtils.isInternalStaff(), request)));
    }

    @PatchMapping("/parking-spots/{id}/status")
    public ResponseEntity<ApiResponse<ParkingSpotResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ParkingSpot.SpotStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated", parkingSpotService.updateStatus(
                id, securityUtils.getCurrentUserId(), securityUtils.isInternalStaff(), status)));
    }

    @DeleteMapping("/parking-spots/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        parkingSpotService.delete(id, securityUtils.getCurrentUserId(), securityUtils.isInternalStaff());
        return ResponseEntity.ok(ApiResponse.ok("Spot deactivated"));
    }
}
