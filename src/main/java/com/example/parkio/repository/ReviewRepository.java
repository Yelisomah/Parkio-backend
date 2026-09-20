package com.example.parkio.repository;

import com.example.parkio.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByParkingLotIdAndVisibleTrue(Long lotId, Pageable pageable);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.parkingLot.id = :lotId AND r.visible = true")
    Double findAverageRatingByLotId(@Param("lotId") Long lotId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.parkingLot.id = :lotId AND r.visible = true")
    long countByLotId(@Param("lotId") Long lotId);
}
