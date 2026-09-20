package com.example.parkio.controller;

import com.example.parkio.dto.request.LoginRequest;
import com.example.parkio.dto.request.RefreshTokenRequest;
import com.example.parkio.dto.request.RegisterRequest;
import com.example.parkio.dto.request.RequestOtpRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.AuthResponse;
import com.example.parkio.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Requests an OTP to verify a phone number before including it in
     * registration — required if (and only if) you want to register with a
     * phone number. No SMS provider is wired into this codebase (see
     * OtpService) — in dev-mode (default) the code comes back directly here.
     */
    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<String>> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        String devModeCode = authService.requestOtp(request.phone());
        String message = devModeCode != null
                ? "OTP (dev-mode, no SMS provider wired): " + devModeCode
                : "OTP sent";
        return ResponseEntity.ok(ApiResponse.ok(message, devModeCode));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authService.refreshToken(request)));
    }

    /**
     * Logout — revokes the current access token so it cannot be reused.
     * The client should discard both tokens after calling this endpoint.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }
}
