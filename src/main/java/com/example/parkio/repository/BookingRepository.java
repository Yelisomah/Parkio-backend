package com.example.parkio.repository;

import com.example.parkio.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Optional<Booking> findByQrCode(String qrCode);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByUserId(Long userId);

    Page<Booking> findByUserIdAndStatus(Long userId, Booking.BookingStatus status, Pageable pageable);

    List<Booking> findBySpotIdAndStatusNotIn(Long spotId, List<Booking.BookingStatus> statuses);

    /**
     * Finds active/upcoming bookings that overlap with a given time window for a spot.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.spot.id = :spotId
              AND b.status NOT IN ('CANCELLED', 'NO_SHOW')
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<Booking> findConflictingBookings(
            @Param("spotId") Long spotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.vehicle
            JOIN FETCH b.spot s
            JOIN FETCH s.parkingLot
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.spot.parkingLot.id = :lotId
              AND b.status = :status
            """)
    Page<Booking> findByLotIdAndStatus(
            @Param("lotId") Long lotId,
            @Param("status") Booking.BookingStatus status,
            Pageable pageable);

    long countByUserIdAndStatus(Long userId, Booking.BookingStatus status);

    /** Used by the no-show sweep: bookings still CONFIRMED whose start time has already passed. */
    List<Booking> findByStatusAndStartTimeBefore(Booking.BookingStatus status, LocalDateTime cutoff);

    /** Used by the reminder job: bookings starting soon that haven't been reminded yet. */
    List<Booking> findByStatusAndReminderSentFalseAndStartTimeBetween(
            Booking.BookingStatus status, LocalDateTime windowStart, LocalDateTime windowEnd);

    // ── Dashboard / analytics queries ─────────────────────────────────────────

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.spot.parkingLot.id = :lotId")
    long countByLotId(@Param("lotId") Long lotId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.spot.parkingLot.id = :lotId AND b.status = :status")
    long countByLotIdAndStatus(@Param("lotId") Long lotId, @Param("status") Booking.BookingStatus status);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Booking b
            WHERE b.spot.parkingLot.id = :lotId
              AND b.status = 'COMPLETED'
            """)
    java.math.BigDecimal sumRevenueByLotId(@Param("lotId") Long lotId);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Booking b
            WHERE b.status = 'COMPLETED'
            """)
    java.math.BigDecimal sumTotalRevenue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    long countByStatus(@Param("status") Booking.BookingStatus status);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.vehicle
            JOIN FETCH b.spot s
            JOIN FETCH s.parkingLot
            WHERE b.spot.parkingLot.id = :lotId
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findByLotId(@Param("lotId") Long lotId, Pageable pageable);

    // ── Report queries ────────────────────────────────────────────────────────

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.spot s
            JOIN FETCH s.parkingLot
            WHERE b.createdAt >= :start AND b.createdAt < :end
            """)
    List<Booking> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.spot.parkingLot.id = :lotId
              AND b.createdAt >= :start AND b.createdAt < :end
            """)
    long countByLotIdInRange(
            @Param("lotId") Long lotId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b
            WHERE b.spot.parkingLot.id = :lotId
              AND b.status = 'COMPLETED'
              AND b.createdAt >= :start AND b.createdAt < :end
            """)
    java.math.BigDecimal sumRevenueByLotIdInRange(
            @Param("lotId") Long lotId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.vehicle
            JOIN FETCH b.spot s
            WHERE s.parkingLot.id = :lotId
              AND b.status IN ('CONFIRMED', 'ACTIVE')
              AND b.qrCode IS NOT NULL
              AND b.startTime < :windowEnd
              AND b.endTime > :windowStart
            """)
    List<Booking> findScannableAtLotInWindow(
            @Param("lotId") Long lotId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);
    @Query("""
            SELECT COALESCE(SUM(b.totalAmount - COALESCE(b.discountAmount, 0)), 0) FROM Booking b
            WHERE b.spot.parkingLot.id = :lotId
              AND b.startTime >= :dayStart AND b.startTime < :dayEnd
              AND b.status <> 'CANCELLED'
            """)
    java.math.BigDecimal sumExpectedForLotOnDay(
            @Param("lotId") Long lotId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.vehicle v
            JOIN FETCH b.spot s
            WHERE v.licensePlate = :plateNumber
              AND b.status IN ('CONFIRMED', 'ACTIVE')
            ORDER BY b.startTime DESC
            """)
    List<Booking> findActiveByPlateNumber(@Param("plateNumber") String plateNumber);
}
