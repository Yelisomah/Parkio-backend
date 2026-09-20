package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parking_lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(length = 20)
    private String zipCode;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private LocalTime openTime;

    private LocalTime closeTime;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String amenities;

    /**
     * Governs which booking flows are legal at this space (Kiro tech-standards.md).
     * BOOKING_ONLY = pre-booking only; STAFFED_REALTIME/HYBRID = warden walk-up
     * and pay_on_exit are also allowed — see BookingService.assertPayOnExitAllowed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ManagementMode managementMode = ManagementMode.BOOKING_ONLY;

    // ── Ownership (design.md: "exactly one of owner_user_id / organization_id
    // is set, never both"; both null = platform-operated by Parkio's internal
    // team directly). Enforced in ParkingLotService as the primary mechanism
    // (environment-agnostic — applies in tests too), backed by a CHECK
    // constraint in the Postgres migration for defense in depth. ──────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * A new listing isn't bookable until approved — design.md task 4.6,
     * applied uniformly to individual owners and companies alike (no lighter
     * path for either, by explicit decision). Existing rows created before
     * this workflow existed are backfilled to APPROVED by the migration —
     * they were already live under the old admin-only model, so demoting
     * them to PENDING_APPROVAL would have silently taken working lots offline.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING_APPROVAL;

    @OneToMany(mappedBy = "parkingLot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkingSpot> spots = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ManagementMode {
        BOOKING_ONLY, STAFFED_REALTIME, HYBRID
    }

    public enum ApprovalStatus {
        PENDING_APPROVAL, APPROVED, CLOSED
    }
}
