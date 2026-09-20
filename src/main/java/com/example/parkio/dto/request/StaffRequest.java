package com.example.parkio.dto.request;

import com.example.parkio.entity.Staff;
import jakarta.validation.constraints.NotNull;

public record StaffRequest(
        @NotNull(message = "userId is required — the platform user being added as staff") Long userId,
        @NotNull(message = "role is required") Staff.StaffRole role,
        /** Optional — must be an existing staff member of the SAME organization; validated in StaffService. */
        Long reportsToStaffId
) {}
