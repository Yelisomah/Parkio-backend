package com.example.parkio.dto.response;

import com.example.parkio.entity.Shift;

import java.time.LocalDateTime;

public record ShiftResponse(
        Long id,
        Long staffId,
        String staffUserName,
        Long spaceId,
        String spaceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Shift.AttendanceStatus attendanceStatus,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime
) {
    public static ShiftResponse from(Shift s) {
        return new ShiftResponse(
                s.getId(),
                s.getStaff().getId(),
                s.getStaff().getUser().getFirstName() + " " + s.getStaff().getUser().getLastName(),
                s.getSpace().getId(),
                s.getSpace().getName(),
                s.getStartTime(),
                s.getEndTime(),
                s.getAttendanceStatus(),
                s.getCheckInTime(),
                s.getCheckOutTime()
        );
    }
}
