package com.example.parkio.repository;

import com.example.parkio.entity.ParkingLot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    Page<ParkingLot> findByActiveTrueAndApprovalStatus(ParkingLot.ApprovalStatus status, Pageable pageable);

    long countByActiveTrue();

    List<ParkingLot> findByCityIgnoreCaseAndActiveTrueAndApprovalStatus(String city, ParkingLot.ApprovalStatus status);

    /** "My lots" — individual owner. */
    List<ParkingLot> findByOwnerUserId(Long ownerUserId);

    /** "My lots" — parking company. */
    List<ParkingLot> findByOrganizationId(Long organizationId);

    /** Ops approval queue. */
    List<ParkingLot> findByApprovalStatusOrderByCreatedAtAsc(ParkingLot.ApprovalStatus status);

    @Query("""
            SELECT pl FROM ParkingLot pl
            WHERE pl.active = true
              AND pl.approvalStatus = 'APPROVED'
              AND (LOWER(pl.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(pl.city) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(pl.address) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<ParkingLot> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT pl FROM ParkingLot pl
            WHERE pl.active = true
              AND pl.approvalStatus = 'APPROVED'
              AND EXISTS (
                SELECT s FROM ParkingSpot s
                WHERE s.parkingLot = pl
                  AND s.status = 'AVAILABLE'
                  AND s.active = true
              )
            """)
    List<ParkingLot> findLotsWithAvailableSpots();

    /**
     * Radius search via PostGIS (`location` column — see V2 migration; NOT a
     * Hibernate-mapped field, maintained by a DB trigger off latitude/longitude).
     * Postgres-only: ST_DWithin/ST_MakePoint have no H2 equivalent, so this is
     * exercised by ParkingLotNearbySearchIT (Testcontainers) rather than the
     * H2-backed unit/integration test suite.
     *
     * Returns id + distance (meters), ordered nearest-first; the service layer
     * batch-fetches the full entities and re-applies this order.
     *
     * @param radiusMeters search radius in meters
     */
    @Query(value = """
            SELECT pl.id AS id, ST_Distance(pl.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS distanceMeters
            FROM parking_lots pl
            WHERE pl.active = true
              AND pl.approval_status = 'APPROVED'
              AND pl.location IS NOT NULL
              AND ST_DWithin(pl.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
            ORDER BY distanceMeters ASC
            """, nativeQuery = true)
    List<NearbyLotProjection> findNearbyWithDistance(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusMeters") double radiusMeters);

    interface NearbyLotProjection {
        Long getId();
        Double getDistanceMeters();
    }
}
