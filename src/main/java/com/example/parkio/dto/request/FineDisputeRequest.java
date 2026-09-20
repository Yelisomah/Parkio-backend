package com.example.parkio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FineDisputeRequest(
        @NotBlank String reason
) {}
