package com.example.parkio.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ShiftRequest(
        @NotNull Long staffId,
        @NotNull Long spaceId,
        @NotNull @Future(message = "startTime must be in the future") LocalDateTime startTime,
        @NotNull LocalDateTime endTime
) {
    @AssertTrue(message = "endTime must be after startTime")
    public boolean isEndAfterStart() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
