package com.example.parkio.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number")
        String phone
) {}
