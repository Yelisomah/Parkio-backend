package com.example.parkio.dto.response;

import com.example.parkio.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long userId,
        String userEmail,
        Long vehicleId,
        String licensePlate,
        Long spotId,
        String spotNumber,
        Long parkingLotId,
        String parkingLotName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime actualCheckIn,
        LocalDateTime actualCheckOut,
        String status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String promoCode,
        String notes,
        String qrCode,
        LocalDateTime createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getBookingReference(),
                b.getUser().getId(),
                b.getUser().getEmail(),
                b.getVehicle().getId(),
                b.getVehicle().getLicensePlate(),
                b.getSpot().getId(),
                b.getSpot().getSpotNumber(),
                b.getSpot().getParkingLot().getId(),
                b.getSpot().getParkingLot().getName(),
                b.getStartTime(),
                b.getEndTime(),
                b.getActualCheckIn(),
                b.getActualCheckOut(),
                b.getStatus().name(),
                b.getTotalAmount(),
                b.getDiscountAmount(),
                b.getPromoCode(),
                b.getNotes(),
                b.getQrCode(),
                b.getCreatedAt()
        );
    }
}
