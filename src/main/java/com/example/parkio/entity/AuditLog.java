package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable record of every significant admin or system action.
 * Never updated — only inserted.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor",  columnList = "actor_email"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_time",   columnList = "created_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email of the user who performed the action. */
    @Column(name = "actor_email", nullable = false, length = 150)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditAction action;

    /** Entity type affected, e.g. "Booking", "User", "ParkingLot". */
    @Column(name = "entity_type", length = 80)
    private String entityType;

    /** Primary key of the affected entity. */
    @Column(name = "entity_id")
    private Long entityId;

    /** Human-readable description. */
    @Column(length = 1000)
    private String description;

    /** Request IP address. */
    @Column(length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AuditAction {
        // Auth
        USER_REGISTERED, USER_LOGIN, USER_LOGOUT, PASSWORD_CHANGED, PASSWORD_RESET,
        // User admin
        USER_ENABLED, USER_DISABLED, USER_DELETED,
        // Booking
        BOOKING_CREATED, BOOKING_CONFIRMED, BOOKING_CANCELLED,
        BOOKING_CHECKED_IN, BOOKING_CHECKED_OUT,
        // Payment
        PAYMENT_PROCESSED, PAYMENT_REFUNDED,
        // Lot / Spot management
        PARKING_LOT_CREATED, PARKING_LOT_UPDATED, PARKING_LOT_DEACTIVATED, PARKING_LOT_ACTIVATED,
        PARKING_SPOT_CREATED, PARKING_SPOT_UPDATED, PARKING_SPOT_DEACTIVATED,
        // Pricing
        PRICING_RULE_CREATED, PRICING_RULE_UPDATED, PRICING_RULE_DELETED,
        // Review
        REVIEW_CREATED, REVIEW_DELETED,
        // Organizations & Staff (Kiro Phase B)
        ORGANIZATION_CREATED, ORGANIZATION_UPDATED,
        STAFF_ADDED, STAFF_ROLE_CHANGED, STAFF_REMOVED,
        STAFF_ASSIGNED_TO_SPACE, STAFF_UNASSIGNED_FROM_SPACE,
        SHIFT_SCHEDULED, SHIFT_CHECKED_IN, SHIFT_CHECKED_OUT, SHIFT_NO_SHOW,
        // QR / scanning (Kiro Phase D)
        SPACE_QR_GENERATED, SCAN_RECORDED, SCAN_RECONCILIATION_MISMATCH,
        // Collections reconciliation & enforcement (Kiro Phase E)
        COLLECTIONS_DISCREPANCY_FLAGGED, FINE_ISSUED, FINE_DISPUTED, FINE_PAID,
        // Promo codes (Kiro Phase F)
        PROMO_CODE_CREATED, PROMO_CODE_DEACTIVATED
    }
}
