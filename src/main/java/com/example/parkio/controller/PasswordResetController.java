package com.example.parkio.controller;

import com.example.parkio.dto.request.ForgotPasswordRequest;
import com.example.parkio.dto.request.ResetPasswordRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Request a password reset email.
     * Always returns 200 to prevent email enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "If that email is registered, a reset link has been sent."));
    }

    /**
     * Complete the password reset using the token from the email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully."));
    }
}
