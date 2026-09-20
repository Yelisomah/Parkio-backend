package com.example.parkio.dto.response;

import com.example.parkio.entity.Vehicle;

import java.time.LocalDateTime;

public record VehicleResponse(
        Long id,
        String licensePlate,
        String make,
        String model,
        String year,
        String color,
        String type,
        Long ownerId,
        LocalDateTime createdAt
) {
    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(
                v.getId(),
                v.getLicensePlate(),
                v.getMake(),
                v.getModel(),
                v.getYear(),
                v.getColor(),
                v.getType().name(),
                v.getOwner().getId(),
                v.getCreatedAt()
        );
    }
}
