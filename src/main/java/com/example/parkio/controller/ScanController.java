package com.example.parkio.controller;

import com.example.parkio.dto.request.OfflineScanBatchRequest;
import com.example.parkio.dto.request.QrScanRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.OfflineSyncEntryResponse;
import com.example.parkio.dto.response.ScanLogResponse;
import com.example.parkio.service.ScanService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;
    private final SecurityUtils securityUtils;

    /** Online scan — the common case (task 8.1). */
    @PostMapping("/online")
    public ResponseEntity<ApiResponse<ScanLogResponse>> scanOnline(
            @PathVariable Long organizationId, @Valid @RequestBody QrScanRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(scanService.scanOnline(organizationId, userId, request)));
    }

    /** Staff app periodically pulls this to build its offline validation cache (task 8.2). */
    @GetMapping("/offline-sync")
    public ResponseEntity<ApiResponse<List<OfflineSyncEntryResponse>>> getOfflineSync(
            @PathVariable Long organizationId, @RequestParam Long spaceId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(scanService.getOfflineSyncRuleset(organizationId, userId, spaceId)));
    }

    /** Staff app calls this on reconnect with everything it recorded offline (task 8.3). */
    @PostMapping("/offline-reconcile")
    public ResponseEntity<ApiResponse<List<ScanLogResponse>>> reconcile(
            @PathVariable Long organizationId, @Valid @RequestBody OfflineScanBatchRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(scanService.reconcileOfflineScans(organizationId, userId, request)));
    }
}
