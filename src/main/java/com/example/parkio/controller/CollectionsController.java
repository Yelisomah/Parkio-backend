package com.example.parkio.controller;

import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.CollectionsSummaryResponse;
import com.example.parkio.service.CollectionsReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only for now — collections discrepancies are a platform-level
 * enforcement concern in this codebase's current model, not scoped per
 * organization (ParkingLot still has no organization ownership — see the
 * StaffAssignment gap noted in Phase B). Revisit once that lands.
 */
@RestController
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPS')")
public class CollectionsController {

    private final CollectionsReconciliationService collectionsReconciliationService;

    /** Flagged discrepancies (task 9.4), optionally filtered to one space. */
    @GetMapping("/flagged")
    public ResponseEntity<ApiResponse<List<CollectionsSummaryResponse>>> getFlagged(
            @RequestParam(required = false) Long spaceId) {
        return ResponseEntity.ok(ApiResponse.ok(collectionsReconciliationService.getFlagged(spaceId)));
    }
}
