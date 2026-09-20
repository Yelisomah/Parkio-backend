package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoleName name;

    /**
     * ROLE_ATTENDANT was previously here but unused (no endpoint/security rule
     * referenced it). Removed rather than built out: the Kiro spec's
     * tech-standards.md explicitly forbids reintroducing a flat "attendant"
     * role/entity — that responsibility belongs to the future Organization →
     * Staff model (org_admin/supervisor/warden), which is a larger migration
     * flagged separately and not done in this pass.
     *
     * NOTE for whoever runs this: any existing DB row with role name
     * ROLE_ATTENDANT will now fail Hibernate's enum validation on read. Either
     * migrate those rows to ROLE_USER or delete them before deploying this
     * change against a database seeded before this commit.
     */
    /**
     * Internal Parkio team roles (distinct from the Organization → Staff
     * model, which is for THIRD-PARTY parking companies' own field staff —
     * warden/supervisor/org_admin. These are Parkio's own employees):
     *   ROLE_SUPPORT    — customer-facing issue resolution
     *   ROLE_OPS        — listing approval, promo codes, collections oversight
     *   ROLE_COMPLIANCE — fine disputes, scan-reconciliation mismatches, fraud review
     *   ROLE_ADMIN       — super-admin: superset of every internal permission
     */
    public enum RoleName {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_SUPPORT,
        ROLE_OPS,
        ROLE_COMPLIANCE
    }
}
