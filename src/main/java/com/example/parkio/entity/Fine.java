package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private ParkingLot space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_staff_id", nullable = false)
    private Staff issuedBy;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "fine_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private boolean paid = false;

    /** Set true once the vehicle owner disputes it (task 11.4) — surfaced on the admin dashboard, not auto-resolved. */
    @Column(nullable = false)
    @Builder.Default
    private boolean disputed = false;

    @Column(name = "dispute_reason", length = 1000)
    private String disputeReason;

    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();
}
