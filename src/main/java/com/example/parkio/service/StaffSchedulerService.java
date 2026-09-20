package com.example.parkio.service;

import com.example.parkio.entity.Shift;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Shift no-show detection (tasks.md 3.5) — mirrors MaintenanceSchedulerService's
 * booking no-show sweep, kept in its own class since it belongs to the
 * Organizations & Staff module, not Booking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffSchedulerService {

    private final ShiftService shiftService;

    @Value("${app.staff.shift-no-show-grace-minutes:20}")
    private long shiftNoShowGraceMinutes;

    /** Every 5 minutes: SCHEDULED shifts whose start time + grace period has elapsed with no check-in → NO_SHOW. */
    @Scheduled(fixedDelayString = "${app.scheduler.shift-no-show-sweep-ms:300000}")
    public void sweepShiftNoShows() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(shiftNoShowGraceMinutes);
        List<Shift> overdue = shiftService.findOverdueForNoShow(cutoff);
        for (Shift shift : overdue) {
            try {
                shiftService.markNoShow(shift);
            } catch (Exception ex) {
                log.error("Failed to mark shift {} as NO_SHOW", shift.getId(), ex);
            }
        }
        if (!overdue.isEmpty()) {
            log.info("Shift no-show sweep: marked {} shift(s) as NO_SHOW", overdue.size());
        }
    }
}
