package com.example.parkio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled — without this, MaintenanceSchedulerService's jobs
 * (no-show sweep, reminders, token/notification cleanup) never fire.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
