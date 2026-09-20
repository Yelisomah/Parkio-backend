package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * Defines a pricing multiplier for a parking lot for a specific
 * time window and/or day-of-week pattern.
 *
 * <p>The multiplier is applied on top of the lot's base hourlyRate:
 * <pre>effectiveRate = lot.hourlyRate × multiplier</pre>
 *
 * <p>Examples:
 * <ul>
 *   <li>Peak hours (08:00–10:00, weekdays) → multiplier = 1.5</li>
 *   <li>Weekend rate (all day Sat/Sun) → multiplier = 0.8</li>
 *   <li>Night discount (22:00–06:00) → multiplier = 0.6</li>
 * </ul>
 */
@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type;

    /**
     * Comma-separated DayOfWeek names, e.g. "MONDAY,TUESDAY".
     * NULL means applies every day.
     */
    @Column(length = 200)
    private String applicableDays;

    private LocalTime startTime;
    private LocalTime endTime;

    /** Rate multiplier, e.g. 1.5 for 50 % surcharge. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal multiplier;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum RuleType {
        PEAK, OFF_PEAK, WEEKEND, HOLIDAY, NIGHT, CUSTOM
    }

    /** Convenience: parse applicableDays into a Set<DayOfWeek>. */
    public Set<DayOfWeek> getDaysOfWeek() {
        if (applicableDays == null || applicableDays.isBlank()) return Set.of(DayOfWeek.values());
        var days = new java.util.HashSet<DayOfWeek>();
        for (String d : applicableDays.split(",")) {
            days.add(DayOfWeek.valueOf(d.trim().toUpperCase()));
        }
        return days;
    }
}
