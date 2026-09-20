package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Short-lived phone OTP. Originally built (as "GuestOtp") only for guest
 * checkout (Phase 2, tasks.md 13.2); generalized here to also gate phone
 * number verification during regular account registration, at explicit
 * request. {@code purpose} keeps the two uses from being interchangeable —
 * a code requested for one purpose can't verify the other, even for the
 * same phone number.
 */
@Entity
@Table(name = "otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;

    /** Hashed with the same PasswordEncoder bean as account passwords — never stored in plaintext. */
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Purpose {
        REGISTRATION, GUEST_CHECKOUT
    }
}
