package com.example.parkio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Nightly collections aggregation (tasks.md 9.2).
 *
 * design.md's tech stack lists Quartz for scheduled jobs; this codebase uses
 * Spring's @Scheduled everywhere else (MaintenanceSchedulerService,
 * StaffSchedulerService), so this follows that established pattern instead
 * of introducing a second scheduling mechanism for just one job. Revisit if
 * clustering/misfire-handling across multiple app instances becomes a real
 * need — that's genuinely where Quartz would earn its place over @Scheduled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionsSchedulerService {

    private final CollectionsReconciliationService collectionsReconciliationService;

    /** Runs just after midnight, aggregates YESTERDAY's collections for every lot. */
    @Scheduled(cron = "${app.scheduler.collections-aggregation-cron:0 15 0 * * *}")
    public void aggregateYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Running nightly collections aggregation for {}", yesterday);
        collectionsReconciliationService.aggregateAllLotsForDate(yesterday);
    }
}
