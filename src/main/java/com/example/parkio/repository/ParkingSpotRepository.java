package com.example.parkio.repository;

import com.example.parkio.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    List<ParkingSpot> findByParkingLotId(Long lotId);

    List<ParkingSpot> findByParkingLotIdAndActiveTrue(Long lotId);

    List<ParkingSpot> findByParkingLotIdAndStatusAndActiveTrue(Long lotId, ParkingSpot.SpotStatus status);

    Optional<ParkingSpot> findByParkingLotIdAndSpotNumber(Long lotId, String spotNumber);

    long countByParkingLotIdAndStatus(Long lotId, ParkingSpot.SpotStatus status);

    long countByParkingLotIdAndActiveTrue(Long lotId);

    long countByActiveTrue();

    long countByStatus(ParkingSpot.SpotStatus status);

    /**
     * Finds spots in a lot that are not booked for the requested time window.
     */
    @Query("""
            SELECT s FROM ParkingSpot s
            WHERE s.parkingLot.id = :lotId
              AND s.active = true
              AND s.status = 'AVAILABLE'
              AND s.id NOT IN (
                SELECT b.spot.id FROM Booking b
                WHERE b.status NOT IN ('CANCELLED', 'NO_SHOW')
                  AND b.startTime < :endTime
                  AND b.endTime > :startTime
              )
            """)
    List<ParkingSpot> findAvailableSpots(
            @Param("lotId") Long lotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
