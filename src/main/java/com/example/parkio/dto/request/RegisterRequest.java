package com.example.parkio.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        /** Optional. If supplied, otpCode must also be supplied and valid — see AuthService.register()'s OTP verification against Otp.Purpose.REGISTRATION. */
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number")
        String phone,

        /** Required if, and only if, phone is supplied — get one from POST /api/v1/auth/otp/request first. */
        String otpCode
) {
    @AssertTrue(message = "Email or phone number is required")
    public boolean isEmailOrPhoneProvided() {
        return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
    }

    @AssertTrue(message = "otpCode is required when phone is supplied")
    public boolean isOtpProvidedWhenPhonePresent() {
        return phone == null || phone.isBlank() || (otpCode != null && !otpCode.isBlank());
    }
}
