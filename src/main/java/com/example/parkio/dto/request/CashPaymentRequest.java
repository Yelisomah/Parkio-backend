package com.example.parkio.dto.request;

import jakarta.validation.constraints.NotNull;

public record CashPaymentRequest(
        @NotNull Long organizationId
) {}
