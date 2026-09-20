package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot spot;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private LocalDateTime actualCheckIn;

    private LocalDateTime actualCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(length = 30)
    private String promoCode;

    /** Set true once the pre-start reminder notification has been sent, so the scheduler never double-sends. */
    @Column(nullable = false)
    @Builder.Default
    private boolean reminderSent = false;

    @Column(length = 500)
    private String notes;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;

    // ── Kiro Phase C additions ──────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InitiatedBy initiatedBy = InitiatedBy.DRIVER_APP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentTiming paymentTiming = PaymentTiming.PREPAID;

    /** Set only for WARDEN_APP bookings — the staff member who created it on the driver's behalf. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_staff_id")
    private Staff createdByStaff;

    /** Generated on confirm() (Kiro design.md: "QR generated" on payment success) — scanned by staff for check-in/check-out (tasks.md task 8). */
    @Column(name = "qr_code", unique = true, length = 40)
    private String qrCode;

    /** Set when this booking is an extension of an earlier one (task 5.4). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extended_from_booking_id")
    private Booking extendedFrom;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum BookingStatus {
        PENDING, CONFIRMED, ACTIVE, COMPLETED, CANCELLED, NO_SHOW,
        /** Kiro Phase C additions — see BookingStateMachine for legal transitions into/out of these. */
        OVERSTAYED, DISPUTED
    }

    public enum InitiatedBy {
        DRIVER_APP, WARDEN_APP, QR_SCAN
    }

    public enum PaymentTiming {
        PREPAID, PAY_ON_EXIT
    }
}
