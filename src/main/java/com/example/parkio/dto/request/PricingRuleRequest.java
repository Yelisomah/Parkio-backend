package com.example.parkio.dto.request;

import com.example.parkio.entity.PricingRule;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public record PricingRuleRequest(
        @NotBlank(message = "Rule name is required")
        @Size(max = 100)
        String name,

        @NotNull(message = "Rule type is required")
        PricingRule.RuleType type,

        /** Comma-separated DayOfWeek names, e.g. "MONDAY,TUESDAY". Null = all days. */
        String applicableDays,

        LocalTime startTime,
        LocalTime endTime,

        @NotNull(message = "Multiplier is required")
        @DecimalMin(value = "0.01", message = "Multiplier must be positive")
        @DecimalMax(value = "10.00", message = "Multiplier cannot exceed 10x")
        BigDecimal multiplier,

        boolean active
) {}
