package com.example.parkio.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromoCodeRequest(
        @NotBlank @Size(max = 30) String code,
        @NotNull @DecimalMin("0.0001") @DecimalMax("1.0") BigDecimal discountPercent,
        @Min(1) Integer maxUses,
        LocalDateTime expiresAt
) {}
