package com.example.parkio.dto.response;

import com.example.parkio.entity.ParkingSpot;

import java.time.LocalDateTime;

public record ParkingSpotResponse(
        Long id,
        String spotNumber,
        String type,
        String status,
        boolean active,
        String notes,
        Long parkingLotId,
        String parkingLotName,
        LocalDateTime createdAt
) {
    public static ParkingSpotResponse from(ParkingSpot s) {
        return new ParkingSpotResponse(
                s.getId(),
                s.getSpotNumber(),
                s.getType().name(),
                s.getStatus().name(),
                s.isActive(),
                s.getNotes(),
                s.getParkingLot().getId(),
                s.getParkingLot().getName(),
                s.getCreatedAt()
        );
    }
}
