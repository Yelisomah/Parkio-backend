package com.example.parkio.service;

import com.example.parkio.dto.response.ReportResponse;
import com.example.parkio.entity.Booking;
import com.example.parkio.entity.ParkingSpot;
import com.example.parkio.repository.BookingRepository;
import com.example.parkio.repository.ParkingLotRepository;
import com.example.parkio.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingRepository bookingRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    @Transactional(readOnly = true)
    public ReportResponse generate(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw com.example.parkio.exception.ParkioException.badRequest("'to' must be on or after 'from'");

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.plusDays(1).atStartOfDay();

        // ── Platform totals ────────────────────────────────────────────────────
        List<Booking> allInRange = bookingRepository.findByDateRange(start, end);

        long totalBookings     = allInRange.size();
        long completedBookings = allInRange.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();
        long cancelledBookings = allInRange.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED).count();
        BigDecimal totalRevenue = allInRange.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Daily revenue breakdown ────────────────────────────────────────────
        List<ReportResponse.DailyRevenue> daily = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate day = d;
            List<Booking> dayBookings = allInRange.stream()
                    .filter(b -> b.getCreatedAt().toLocalDate().equals(day))
                    .toList();
            BigDecimal dayRevenue = dayBookings.stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
                    .map(Booking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            daily.add(new ReportResponse.DailyRevenue(day, dayRevenue, dayBookings.size()));
        }

        // ── Per-lot occupancy ──────────────────────────────────────────────────
        List<ReportResponse.LotOccupancy> lotOccupancy = parkingLotRepository.findAll()
                .stream()
                .map(lot -> {
                    long totalSpots  = parkingSpotRepository.countByParkingLotIdAndActiveTrue(lot.getId());
                    long lotBookings = bookingRepository.countByLotIdInRange(lot.getId(), start, end);
                    BigDecimal lotRevenue = bookingRepository.sumRevenueByLotIdInRange(lot.getId(), start, end);

                    // occupancy = completed bookings hours / (spots × range hours)
                    long rangeHours = java.time.Duration.between(start, end).toHours();
                    double maxHours = totalSpots * rangeHours;
                    double usedHours = allInRange.stream()
                            .filter(b -> b.getSpot().getParkingLot().getId().equals(lot.getId()))
                            .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
                            .mapToDouble(b -> java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes() / 60.0)
                            .sum();
                    double occupancy = maxHours > 0 ? Math.min(usedHours / maxHours, 1.0) : 0.0;

                    return new ReportResponse.LotOccupancy(
                            lot.getId(), lot.getName(), totalSpots,
                            Math.round(occupancy * 1000.0) / 1000.0,
                            lotRevenue != null ? lotRevenue : BigDecimal.ZERO);
                })
                .toList();

        return new ReportResponse(from, to, totalRevenue,
                totalBookings, completedBookings, cancelledBookings,
                daily, lotOccupancy);
    }
}
