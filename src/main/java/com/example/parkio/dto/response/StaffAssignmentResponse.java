package com.example.parkio.dto.response;

import com.example.parkio.entity.StaffAssignment;

import java.time.LocalDateTime;

public record StaffAssignmentResponse(
        Long id,
        Long staffId,
        String staffUserName,
        Long spaceId,
        String spaceName,
        LocalDateTime assignedAt,
        boolean active
) {
    public static StaffAssignmentResponse from(StaffAssignment a) {
        return new StaffAssignmentResponse(
                a.getId(),
                a.getStaff().getId(),
                a.getStaff().getUser().getFirstName() + " " + a.getStaff().getUser().getLastName(),
                a.getSpace().getId(),
                a.getSpace().getName(),
                a.getAssignedAt(),
                a.isActive()
        );
    }
}
