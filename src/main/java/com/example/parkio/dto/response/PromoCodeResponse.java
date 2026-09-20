package com.example.parkio.dto.response;

import com.example.parkio.entity.PromoCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromoCodeResponse(
        Long id,
        String code,
        BigDecimal discountPercent,
        Integer maxUses,
        int usesCount,
        boolean active,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static PromoCodeResponse from(PromoCode p) {
        return new PromoCodeResponse(
                p.getId(), p.getCode(), p.getDiscountPercent(), p.getMaxUses(),
                p.getUsesCount(), p.isActive(), p.getExpiresAt(), p.getCreatedAt());
    }
}
