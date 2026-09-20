package com.example.parkio.dto.request;

import jakarta.validation.constraints.NotNull;

public record StaffAssignmentRequest(
        @NotNull Long staffId,
        @NotNull Long spaceId
) {}
