package com.example.parkio.dto.response;

import com.example.parkio.entity.Fine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FineResponse(
        Long id,
        String plateNumber,
        Long spaceId,
        String spaceName,
        Long issuedByStaffId,
        String issuedByName,
        String reason,
        BigDecimal fineAmount,
        boolean paid,
        boolean disputed,
        String disputeReason,
        LocalDateTime issuedAt
) {
    public static FineResponse from(Fine f) {
        return new FineResponse(
                f.getId(),
                f.getPlateNumber(),
                f.getSpace().getId(),
                f.getSpace().getName(),
                f.getIssuedBy().getId(),
                f.getIssuedBy().getUser().getFirstName() + " " + f.getIssuedBy().getUser().getLastName(),
                f.getReason(),
                f.getFineAmount(),
                f.isPaid(),
                f.isDisputed(),
                f.getDisputeReason(),
                f.getIssuedAt()
        );
    }
}
