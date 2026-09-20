package com.example.parkio.service;

import com.example.parkio.entity.Booking;
import com.example.parkio.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background jobs that previously didn't exist at all:
 *  - No-show sweep      (issue #9)
 *  - Reset-token cleanup (issue #10)
 *  - Booking reminders   (issue #11)
 *  - Notification cleanup (issue #14)
 *
 * All run on the default @Scheduled thread pool. Fine for this workload —
 * move to Quartz (or the Kiro-spec target scheduler) if jobs need clustering
 * / persistence / misfire handling across multiple app instances later.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceSchedulerService {

    private final BookingService bookingService;
    private final PasswordResetService passwordResetService;
    private final NotificationService notificationService;

    @Value("${app.booking.no-show-grace-minutes:30}")
    private long noShowGraceMinutes;

    @Value("${app.booking.reminder-minutes-before:60}")
    private long reminderMinutesBefore;

    @Value("${app.notification.retention-days:90}")
    private long notificationRetentionDays;

    /** Every 5 minutes: CONFIRMED bookings whose check-in grace window has elapsed → NO_SHOW. */
    @Scheduled(fixedDelayString = "${app.scheduler.no-show-sweep-ms:300000}")
    public void sweepNoShows() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(noShowGraceMinutes);
        List<Booking> overdue = bookingService.findOverdueForNoShow(cutoff);
        for (Booking booking : overdue) {
            try {
                bookingService.markNoShow(booking);
            } catch (Exception ex) {
                log.error("Failed to mark booking {} as NO_SHOW", booking.getId(), ex);
            }
        }
        if (!overdue.isEmpty()) {
            log.info("No-show sweep: marked {} booking(s) as NO_SHOW", overdue.size());
        }
    }

    /** Every 10 minutes: send a reminder for CONFIRMED bookings starting soon. */
    @Scheduled(fixedDelayString = "${app.scheduler.reminder-sweep-ms:600000}")
    public void sendBookingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now;
        LocalDateTime windowEnd = now.plusMinutes(reminderMinutesBefore);

        List<Booking> due = bookingService.findDueForReminder(windowStart, windowEnd);
        for (Booking booking : due) {
            try {
                notificationService.send(booking.getUser().getId(),
                        Notification.NotificationType.BOOKING_REMINDER,
                        "Upcoming Booking",
                        "Your booking " + booking.getBookingReference() + " at " +
                                booking.getSpot().getParkingLot().getName() + " starts at " + booking.getStartTime() + ".",
                        booking.getId(), "Booking");
                bookingService.markReminderSent(booking);
            } catch (Exception ex) {
                log.error("Failed to send reminder for booking {}", booking.getId(), ex);
            }
        }
        if (!due.isEmpty()) {
            log.info("Reminder sweep: sent {} reminder(s)", due.size());
        }
    }

    /** Daily at 03:00: purge expired password reset tokens (PasswordResetTokenRepository.deleteExpired() was never called before this). */
    @Scheduled(cron = "${app.scheduler.password-reset-cleanup-cron:0 0 3 * * *}")
    public void cleanupExpiredPasswordResetTokens() {
        int deleted = passwordResetService.cleanupExpiredTokens();
        if (deleted > 0) {
            log.info("Cleaned up {} expired password reset token(s)", deleted);
        }
    }

    /** Daily at 03:30: purge old read notifications so the table doesn't grow unbounded. */
    @Scheduled(cron = "${app.scheduler.notification-cleanup-cron:0 30 3 * * *}")
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(notificationRetentionDays);
        int deleted = notificationService.deleteReadOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} old read notification(s)", deleted);
        }
    }
}
