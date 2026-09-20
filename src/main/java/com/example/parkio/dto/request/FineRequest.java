package com.example.parkio.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FineRequest(
        @NotNull Long organizationId,
        @NotNull Long spaceId,
        @NotBlank String plateNumber,
        @NotBlank String reason,
        @NotNull @DecimalMin(value = "0.01") BigDecimal fineAmount
) {}
