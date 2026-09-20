package com.example.parkio.dto.response;

import com.example.parkio.entity.ParkingLot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ParkingLotResponse(
        Long id,
        String name,
        String address,
        String city,
        String state,
        String zipCode,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal hourlyRate,
        BigDecimal dailyRate,
        boolean active,
        LocalTime openTime,
        LocalTime closeTime,
        String description,
        String amenities,
        ParkingLot.ManagementMode managementMode,
        Long ownerUserId,
        String ownerName,
        Long organizationId,
        String organizationName,
        ParkingLot.ApprovalStatus approvalStatus,
        long totalSpots,
        long availableSpots,
        LocalDateTime createdAt
) {
    public static ParkingLotResponse from(ParkingLot lot, long total, long available) {
        return new ParkingLotResponse(
                lot.getId(),
                lot.getName(),
                lot.getAddress(),
                lot.getCity(),
                lot.getState(),
                lot.getZipCode(),
                lot.getLatitude(),
                lot.getLongitude(),
                lot.getHourlyRate(),
                lot.getDailyRate(),
                lot.isActive(),
                lot.getOpenTime(),
                lot.getCloseTime(),
                lot.getDescription(),
                lot.getAmenities(),
                lot.getManagementMode(),
                lot.getOwnerUser() != null ? lot.getOwnerUser().getId() : null,
                lot.getOwnerUser() != null ? lot.getOwnerUser().getFirstName() + " " + lot.getOwnerUser().getLastName() : null,
                lot.getOrganization() != null ? lot.getOrganization().getId() : null,
                lot.getOrganization() != null ? lot.getOrganization().getName() : null,
                lot.getApprovalStatus(),
                total,
                available,
                lot.getCreatedAt()
        );
    }
}
