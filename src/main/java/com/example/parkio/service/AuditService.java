package com.example.parkio.service;

import com.example.parkio.entity.AuditLog;
import com.example.parkio.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes audit log entries asynchronously so they never block
 * the calling transaction. The log insert runs in its own
 * REQUIRES_NEW transaction so it persists even if the caller rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actorEmail,
                    AuditLog.AuditAction action,
                    String entityType,
                    Long entityId,
                    String description,
                    String ipAddress) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorEmail(actorEmail != null ? actorEmail : "system")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // Audit failures must never crash the application
            log.error("Failed to write audit log: action={}, entity={}/{}", action, entityType, entityId, ex);
        }
    }

    /** Convenience overload without IP. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actorEmail,
                    AuditLog.AuditAction action,
                    String entityType,
                    Long entityId,
                    String description) {
        log(actorEmail, action, entityType, entityId, description, null);
    }

    // ── Admin read-only queries ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<com.example.parkio.dto.response.AuditLogResponse> getAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(com.example.parkio.dto.response.AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<com.example.parkio.dto.response.AuditLogResponse> getByActor(String email, Pageable pageable) {
        return auditLogRepository.findByActorEmail(email, pageable)
                .map(com.example.parkio.dto.response.AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<com.example.parkio.dto.response.AuditLogResponse> getByEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(com.example.parkio.dto.response.AuditLogResponse::from);
    }
}
