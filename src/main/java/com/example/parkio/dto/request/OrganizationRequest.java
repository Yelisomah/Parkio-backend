package com.example.parkio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 20) String contactPhone
) {}
