package com.example.parkio.dto.request;

import com.example.parkio.entity.Staff;
import jakarta.validation.constraints.NotNull;

public record StaffRoleUpdateRequest(
        @NotNull Staff.StaffRole role
) {}
