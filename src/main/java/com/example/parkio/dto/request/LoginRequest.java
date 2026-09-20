package com.example.parkio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Email or phone number is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
