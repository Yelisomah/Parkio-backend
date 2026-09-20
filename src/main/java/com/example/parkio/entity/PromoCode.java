package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Real promo code with usage caps (tasks.md task 10), replacing the
 * hardcoded two-entry table in the original DiscountService.
 */
@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /** e.g. 0.10 for 10% off. */
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 4)
    private BigDecimal discountPercent;

    /** Null = unlimited uses. */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "uses_count", nullable = false)
    @Builder.Default
    private int usesCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Null = never expires. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
