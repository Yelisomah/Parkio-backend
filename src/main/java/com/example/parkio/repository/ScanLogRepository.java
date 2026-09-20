package com.example.parkio.repository;

import com.example.parkio.entity.ScanLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {
    List<ScanLog> findByBookingIdOrderByScanTimeDesc(Long bookingId);
    List<ScanLog> findByReconciliationStatus(ScanLog.ReconciliationStatus status);
}
