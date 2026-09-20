package com.example.parkio.dto.response;

import com.example.parkio.entity.Organization;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        String contactPhone,
        Long ownerUserId,
        String ownerName,
        boolean active,
        LocalDateTime createdAt
) {
    public static OrganizationResponse from(Organization o) {
        return new OrganizationResponse(
                o.getId(),
                o.getName(),
                o.getContactPhone(),
                o.getOwner().getId(),
                o.getOwner().getFirstName() + " " + o.getOwner().getLastName(),
                o.isActive(),
                o.getCreatedAt()
        );
    }
}
