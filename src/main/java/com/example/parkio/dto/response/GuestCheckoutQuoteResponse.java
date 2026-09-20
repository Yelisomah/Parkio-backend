package com.example.parkio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A no-side-effect price/availability preview before the guest commits (task 13.1). */
public record GuestCheckoutQuoteResponse(
        Long spaceId,
        String spaceName,
        boolean spotAvailable,
        BigDecimal estimatedTotal,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}
