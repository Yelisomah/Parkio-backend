package com.example.parkio.repository;

import com.example.parkio.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findByStaffIdOrderByStartTimeDesc(Long staffId);

    List<Shift> findBySpaceIdAndStartTimeBetween(Long spaceId, LocalDateTime from, LocalDateTime to);

    /** Used by the no-show sweep: shifts still SCHEDULED whose start time has already passed the grace window. */
    List<Shift> findByAttendanceStatusAndStartTimeBefore(Shift.AttendanceStatus status, LocalDateTime cutoff);

    @Query("SELECT s FROM Shift s WHERE s.staff.id = :staffId AND s.attendanceStatus = 'SCHEDULED' " +
           "AND :now BETWEEN s.startTime AND s.endTime")
    List<Shift> findActiveScheduledShiftForStaff(@Param("staffId") Long staffId, @Param("now") LocalDateTime now);
}
