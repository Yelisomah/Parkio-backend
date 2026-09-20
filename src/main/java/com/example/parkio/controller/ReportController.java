package com.example.parkio.controller;

import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.ReportResponse;
import com.example.parkio.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Generates a full report for a date range including:
     * - Platform totals (revenue, bookings, cancellations)
     * - Daily revenue breakdown
     * - Per-lot occupancy rates
     *
     * Example: GET /api/v1/admin/reports?from=2026-01-01&to=2026-01-31
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ReportResponse>> generate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.generate(from, to)));
    }
}
