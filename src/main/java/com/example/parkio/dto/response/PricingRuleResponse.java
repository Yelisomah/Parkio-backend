package com.example.parkio.dto.response;

import com.example.parkio.entity.PricingRule;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record PricingRuleResponse(
        Long id,
        Long parkingLotId,
        String parkingLotName,
        String name,
        String type,
        String applicableDays,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal multiplier,
        boolean active,
        LocalDateTime createdAt
) {
    public static PricingRuleResponse from(PricingRule r) {
        return new PricingRuleResponse(
                r.getId(),
                r.getParkingLot().getId(),
                r.getParkingLot().getName(),
                r.getName(),
                r.getType().name(),
                r.getApplicableDays(),
                r.getStartTime(),
                r.getEndTime(),
                r.getMultiplier(),
                r.isActive(),
                r.getCreatedAt()
        );
    }
}
