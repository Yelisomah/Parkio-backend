package com.example.parkio.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        // ── Platform-level totals ──────────────────────────────────────────────
        long totalUsers,
        long totalLots,
        long totalSpots,
        long availableSpots,

        // ── Booking totals ────────────────────────────────────────────────────
        long totalBookings,
        long pendingBookings,
        long activeBookings,
        long completedBookings,
        long cancelledBookings,

        // ── Revenue ───────────────────────────────────────────────────────────
        BigDecimal totalRevenue,

        // ── Per-lot breakdown ─────────────────────────────────────────────────
        List<LotSummary> lots
) {
    public record LotSummary(
            Long lotId,
            String lotName,
            String city,
            long totalSpots,
            long availableSpots,
            long totalBookings,
            long activeBookings,
            long completedBookings,
            BigDecimal revenue
    ) {}
}
