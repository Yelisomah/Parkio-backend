package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parking_spots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"parking_lot_id", "spotNumber"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String spotNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SpotStatus status = SpotStatus.AVAILABLE;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 200)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;

    @OneToMany(mappedBy = "spot", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum SpotType {
        STANDARD, COMPACT, HANDICAPPED, ELECTRIC, MOTORCYCLE, OVERSIZED
    }

    public enum SpotStatus {
        AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE
    }
}
