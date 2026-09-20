package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Links a {@link Staff} member to a space ({@link ParkingLot} — this
 * codebase's "space" equivalent) they're assigned to work.
 *
 * NOTE (flagged, not silently assumed): {@link ParkingLot} has no
 * organization/owner concept yet — that's Listings-module scope (tasks.md
 * task 4, sequenced after this one), where design.md's ParkingSpace has
 * owner_user_id XOR organization_id. Until that lands, this assignment does
 * NOT validate that the space belongs to the assigning staff's organization
 * — any active ParkingLot can be assigned. Tighten this once ParkingLot
 * carries an organization_id.
 */
@Entity
@Table(name = "staff_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private ParkingLot space;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
