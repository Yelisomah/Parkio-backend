package com.example.parkio.controller;

import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.BookingResponse;
import com.example.parkio.dto.response.DashboardResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /**
     * Platform overview: totals for users, lots, spots, bookings and revenue,
     * plus a per-lot breakdown.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getOverview()));
    }

    /**
     * Paginated list of all bookings for a specific parking lot.
     */
    @GetMapping("/lots/{lotId}/bookings")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getBookingsByLot(
            @PathVariable Long lotId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getBookingsByLot(lotId, pageable)));
    }
}
