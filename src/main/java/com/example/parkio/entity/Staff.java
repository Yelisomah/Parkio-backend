package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A person belonging to an {@link Organization} with role org_admin,
 * supervisor, or warden.
 *
 * Per tech-standards.md: "Do not reintroduce a separate 'attendant' entity;
 * it has been fully replaced by staff." This IS that replacement.
 */
@Entity
@Table(name = "staff", uniqueConstraints = {
        @UniqueConstraint(name = "uq_staff_org_user", columnNames = {"organization_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    /** Management hierarchy — must belong to the SAME organization; enforced in StaffService, not the DB. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_staff_id")
    private Staff reportsTo;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum StaffRole {
        ORG_ADMIN, SUPERVISOR, WARDEN
    }
}
