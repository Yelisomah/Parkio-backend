package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A scheduled attendance window for a {@link Staff} member at a space
 * ({@link ParkingLot}). Attendance is tracked explicitly (not inferred)
 * so the no-show sweep (tasks.md 3.5) has something concrete to act on.
 */
@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private ParkingLot space;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    @Builder.Default
    private AttendanceStatus attendanceStatus = AttendanceStatus.SCHEDULED;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * SCHEDULED is an addition on top of design.md's [checked_in|no_show|completed] —
     * needed to represent "hasn't started yet" before any of those three apply.
     */
    public enum AttendanceStatus {
        SCHEDULED, CHECKED_IN, NO_SHOW, COMPLETED
    }
}
