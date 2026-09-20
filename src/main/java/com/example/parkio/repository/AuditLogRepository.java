package com.example.parkio.repository;

import com.example.parkio.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActorEmail(String actorEmail, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);
}
