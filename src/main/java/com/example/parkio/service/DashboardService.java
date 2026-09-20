package com.example.parkio.service;

import com.example.parkio.dto.response.BookingResponse;
import com.example.parkio.dto.response.DashboardResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.entity.Booking;
import com.example.parkio.entity.ParkingSpot;
import com.example.parkio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getOverview() {

        long totalUsers  = userRepository.count();
        long totalLots   = parkingLotRepository.countByActiveTrue();
        long totalSpots  = parkingSpotRepository.countByActiveTrue();
        long available   = parkingSpotRepository.countByStatus(ParkingSpot.SpotStatus.AVAILABLE);

        long totalBookings     = bookingRepository.count();
        long pendingBookings   = bookingRepository.countByStatus(Booking.BookingStatus.PENDING);
        long activeBookings    = bookingRepository.countByStatus(Booking.BookingStatus.ACTIVE);
        long completedBookings = bookingRepository.countByStatus(Booking.BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(Booking.BookingStatus.CANCELLED);

        var totalRevenue = bookingRepository.sumTotalRevenue();

        List<DashboardResponse.LotSummary> lots = parkingLotRepository.findAll()
                .stream()
                .map(lot -> {
                    long lotTotal     = parkingSpotRepository.countByParkingLotIdAndActiveTrue(lot.getId());
                    long lotAvail     = parkingSpotRepository.countByParkingLotIdAndStatus(
                                            lot.getId(), ParkingSpot.SpotStatus.AVAILABLE);
                    long lotBookings  = bookingRepository.countByLotId(lot.getId());
                    long lotActive    = bookingRepository.countByLotIdAndStatus(
                                            lot.getId(), Booking.BookingStatus.ACTIVE);
                    long lotCompleted = bookingRepository.countByLotIdAndStatus(
                                            lot.getId(), Booking.BookingStatus.COMPLETED);
                    var  lotRevenue   = bookingRepository.sumRevenueByLotId(lot.getId());

                    return new DashboardResponse.LotSummary(
                            lot.getId(), lot.getName(), lot.getCity(),
                            lotTotal, lotAvail,
                            lotBookings, lotActive, lotCompleted,
                            lotRevenue);
                })
                .toList();

        return new DashboardResponse(
                totalUsers, totalLots, totalSpots, available,
                totalBookings, pendingBookings, activeBookings,
                completedBookings, cancelledBookings,
                totalRevenue, lots);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getBookingsByLot(Long lotId, Pageable pageable) {
        return PageResponse.from(
                bookingRepository.findByLotId(lotId, pageable).map(BookingResponse::from));
    }
}
