package com.example.parkio.dto.response;

import com.example.parkio.entity.DailyCollectionsSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CollectionsSummaryResponse(
        Long id,
        Long spaceId,
        String spaceName,
        Long staffId,
        String staffName,
        LocalDate summaryDate,
        BigDecimal expectedAmount,
        BigDecimal digitalAmount,
        BigDecimal cashLoggedAmount,
        boolean discrepancyFlag,
        String discrepancyNote,
        LocalDateTime createdAt
) {
    public static CollectionsSummaryResponse from(DailyCollectionsSummary d) {
        return new CollectionsSummaryResponse(
                d.getId(),
                d.getSpace().getId(),
                d.getSpace().getName(),
                d.getStaff() != null ? d.getStaff().getId() : null,
                d.getStaff() != null ? d.getStaff().getUser().getFirstName() + " " + d.getStaff().getUser().getLastName() : null,
                d.getSummaryDate(),
                d.getExpectedAmount(),
                d.getDigitalAmount(),
                d.getCashLoggedAmount(),
                d.isDiscrepancyFlag(),
                d.getDiscrepancyNote(),
                d.getCreatedAt()
        );
    }
}
