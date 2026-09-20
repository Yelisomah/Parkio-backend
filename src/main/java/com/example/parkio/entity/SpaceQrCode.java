package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A permanent QR code identifying a space (ParkingLot). Scanning it resolves
 * to basic space info (tasks.md 4.5) — it does NOT start a booking; that's
 * the QR-signage guest walk-up flow, explicitly Phase 2 per tech-standards.md.
 */
@Entity
@Table(name = "space_qr_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceQrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false, unique = true)
    private ParkingLot space;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
