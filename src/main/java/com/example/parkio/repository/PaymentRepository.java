package com.example.parkio.repository;

import com.example.parkio.entity.Payment;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByGatewayReference(String gatewayReference);

    Page<Payment> findByBookingUserId(Long userId, Pageable pageable);

    boolean existsByTransactionId(String transactionId);

    /** Collections reconciliation (task 9): completed payments for a lot within a day, split later by payment method in the service. */
    @Query("""
            SELECT p FROM Payment p
            JOIN p.booking b
            WHERE b.spot.parkingLot.id = :lotId
              AND p.status = 'COMPLETED'
              AND p.paidAt >= :dayStart AND p.paidAt < :dayEnd
            """)
    List<Payment> findCompletedForLotOnDay(
            @Param("lotId") Long lotId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}