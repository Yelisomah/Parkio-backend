package com.example.parkio.dto.response;

import com.example.parkio.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String entityType,
        Long entityId,
        String description,
        String ipAddress,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorEmail(),
                log.getAction().name(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
