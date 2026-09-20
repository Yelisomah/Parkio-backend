package com.example.parkio.repository;

import com.example.parkio.entity.DailyCollectionsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyCollectionsSummaryRepository extends JpaRepository<DailyCollectionsSummary, Long> {

    Optional<DailyCollectionsSummary> findBySpaceIdAndStaffIsNullAndSummaryDate(Long spaceId, LocalDate date);

    Optional<DailyCollectionsSummary> findBySpaceIdAndStaffIdAndSummaryDate(Long spaceId, Long staffId, LocalDate date);

    /** Trailing window for the variance calc (task 9.3) — space-level (staff IS NULL) rows only, most recent first. */
    @Query("""
            SELECT d FROM DailyCollectionsSummary d
            WHERE d.space.id = :spaceId AND d.staff IS NULL AND d.summaryDate < :beforeDate
            ORDER BY d.summaryDate DESC
            """)
    List<DailyCollectionsSummary> findTrailingForSpace(@Param("spaceId") Long spaceId, @Param("beforeDate") LocalDate beforeDate);

    List<DailyCollectionsSummary> findByDiscrepancyFlagTrueOrderBySummaryDateDesc();

    List<DailyCollectionsSummary> findBySpaceIdAndDiscrepancyFlagTrueOrderBySummaryDateDesc(Long spaceId);
}
