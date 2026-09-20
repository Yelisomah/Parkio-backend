package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Advertising slot tied to a space or an organization (design.md doesn't
 * state an exclusivity rule for Ad the way it does for ParkingSpace, so
 * both are left nullable without an XOR constraint — matching the spec as
 * written rather than assuming one).
 *
 * NOTE: tasks.md 11.1 only calls for creating this entity + migration; no
 * CRUD service/controller is in the task list, so none was built. Add one
 * when there's an actual requirement driving its shape (e.g. who can create
 * ads, approval workflow, pricing) rather than guessing now.
 */
@Entity
@Table(name = "ads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private ParkingLot space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
