package com.example.parkio.dto.response;

import com.example.parkio.entity.Staff;

import java.time.LocalDateTime;

public record StaffResponse(
        Long id,
        Long organizationId,
        Long userId,
        String userName,
        String userEmail,
        Staff.StaffRole role,
        Long reportsToStaffId,
        String reportsToName,
        boolean active,
        LocalDateTime createdAt
) {
    public static StaffResponse from(Staff s) {
        return new StaffResponse(
                s.getId(),
                s.getOrganization().getId(),
                s.getUser().getId(),
                s.getUser().getFirstName() + " " + s.getUser().getLastName(),
                s.getUser().getEmail(),
                s.getRole(),
                s.getReportsTo() != null ? s.getReportsTo().getId() : null,
                s.getReportsTo() != null
                        ? s.getReportsTo().getUser().getFirstName() + " " + s.getReportsTo().getUser().getLastName()
                        : null,
                s.isActive(),
                s.getCreatedAt()
        );
    }
}
