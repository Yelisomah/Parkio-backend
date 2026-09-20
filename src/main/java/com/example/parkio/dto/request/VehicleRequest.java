package com.example.parkio.dto.request;

import com.example.parkio.entity.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehicleRequest(

        @NotBlank(message = "License plate is required")
        @Size(max = 20, message = "License plate must not exceed 20 characters")
        String licensePlate,

        @NotBlank(message = "Make is required")
        @Size(max = 50)
        String make,

        @NotBlank(message = "Model is required")
        @Size(max = 50)
        String model,

        @NotBlank(message = "Year is required")
        @Pattern(regexp = "^[0-9]{4}$", message = "Year must be a 4-digit number")
        String year,

        @Size(max = 30)
        String color,

        @NotNull(message = "Vehicle type is required")
        Vehicle.VehicleType type
) {}
