package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Nightly rollup of a space's collections for one day (tasks.md task 9).
 *
 * Adaptation flagged: design.md aggregates "digital + cash_logged per space
 * per staff per day", but in this codebase's Payment model, digital
 * (card/mobile-money) payments aren't staff-attributed at all — the driver
 * pays directly, no staff involved. Only CASH payments carry a staff
 * attribution (via the logging staff member). So in practice:
 *   - the {@code staff} = null row for a given (space, date) holds the
 *     space's total DIGITAL revenue that day (no staff dimension possible),
 *   - a {@code staff} != null row holds that specific staff member's total
 *     CASH-logged revenue that day.
 * discrepancyFlag/trailing-average comparison (task 9.3) is computed on the
 * space-level (staff = null) row, per design.md's flow ("compares the day's
 * total against a trailing average for THAT SPACE").
 */
@Entity
@Table(name = "daily_collections_summary", uniqueConstraints = {
        @UniqueConstraint(name = "uq_collections_space_staff_date", columnNames = {"space_id", "staff_id", "summary_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCollectionsSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private ParkingLot space;

    /** Null = space-level digital-revenue row. Set = that staff member's cash-logged total for the day. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "expected_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "digital_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal digitalAmount = BigDecimal.ZERO;

    @Column(name = "cash_logged_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cashLoggedAmount = BigDecimal.ZERO;

    @Column(name = "discrepancy_flag", nullable = false)
    @Builder.Default
    private boolean discrepancyFlag = false;

    @Column(name = "discrepancy_note", length = 500)
    private String discrepancyNote;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
