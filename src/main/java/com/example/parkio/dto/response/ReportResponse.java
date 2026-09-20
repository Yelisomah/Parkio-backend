package com.example.parkio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        long totalBookings,
        long completedBookings,
        long cancelledBookings,
        List<DailyRevenue> dailyRevenue,
        List<LotOccupancy> lotOccupancy
) {
    public record DailyRevenue(
            LocalDate date,
            BigDecimal revenue,
            long bookings
    ) {}

    public record LotOccupancy(
            Long lotId,
            String lotName,
            long totalSpots,
            double occupancyRate,   // 0.0–1.0
            BigDecimal revenue
    ) {}
}
