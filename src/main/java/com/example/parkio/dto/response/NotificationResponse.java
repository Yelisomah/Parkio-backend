package com.example.parkio.dto.response;

import com.example.parkio.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        boolean read,
        Long referenceId,
        String referenceType,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getReferenceId(),
                n.getReferenceType(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
