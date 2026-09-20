package com.example.parkio.repository;

import com.example.parkio.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    /**
     * Atomically claims one use, only if the code is still active, unexpired,
     * and under its cap — this is the actual cap enforcement (tasks.md 10.2:
     * "redemption-with-cap-enforcement"). A plain read-then-check-then-write
     * in the service layer would race under concurrent redemptions right at
     * the cap boundary; this single UPDATE...WHERE is atomic at the DB level.
     * Returns the number of rows updated: 1 = claimed, 0 = rejected (already
     * inactive/expired/at cap) — the caller must check this, not assume success.
     */
    @Modifying
    @Query("""
            UPDATE PromoCode p SET p.usesCount = p.usesCount + 1
            WHERE p.id = :id
              AND p.active = true
              AND (p.expiresAt IS NULL OR p.expiresAt > :now)
              AND (p.maxUses IS NULL OR p.usesCount < p.maxUses)
            """)
    int tryClaimUse(@Param("id") Long id, @Param("now") LocalDateTime now);
}
