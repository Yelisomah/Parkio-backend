package com.example.parkio.dto.response;

import com.example.parkio.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long bookingId,
        String bookingReference,
        Long userId,
        String userFullName,
        Long parkingLotId,
        String parkingLotName,
        int rating,
        String comment,
        boolean visible,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getBooking().getId(),
                r.getBooking().getBookingReference(),
                r.getUser().getId(),
                r.getUser().getFirstName() + " " + r.getUser().getLastName(),
                r.getParkingLot().getId(),
                r.getParkingLot().getName(),
                r.getRating(),
                r.getComment(),
                r.isVisible(),
                r.getCreatedAt()
        );
    }
}
