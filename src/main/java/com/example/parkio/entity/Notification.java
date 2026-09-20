package com.example.parkio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_read", columnList = "is_read")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** Optional link to the related entity (e.g. booking ID). */
    private Long referenceId;

    @Column(length = 50)
    private String referenceType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    public enum NotificationType {
        BOOKING_CREATED,
        BOOKING_CONFIRMED,
        BOOKING_CANCELLED,
        BOOKING_REMINDER,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED,
        CHECK_IN_REMINDER,
        FINE_ISSUED,
        ACCOUNT_DISABLED,
        GENERAL
    }
}
