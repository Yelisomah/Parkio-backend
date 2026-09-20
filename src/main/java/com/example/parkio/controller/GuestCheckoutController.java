package com.example.parkio.controller;

import com.example.parkio.dto.request.GuestCheckoutRequest;
import com.example.parkio.dto.request.RequestOtpRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.GuestCheckoutQuoteResponse;
import com.example.parkio.dto.response.GuestCheckoutResponse;
import com.example.parkio.service.GuestCheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * All public (no JWT) — a guest scanning a physical QR sign has no account
 * yet. OTP verification in confirm() is what proves identity; see
 * SecurityConfig for the permitAll rule this needs.
 */
@RestController
@RequestMapping("/api/v1/guest-checkout")
@RequiredArgsConstructor
public class GuestCheckoutController {

    private final GuestCheckoutService guestCheckoutService;

    /** No side effects — lets the guest see price/availability before committing (task 13.1). */
    @GetMapping("/{qrCode}/quote")
    public ResponseEntity<ApiResponse<GuestCheckoutQuoteResponse>> getQuote(
            @PathVariable String qrCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(ApiResponse.ok(guestCheckoutService.getQuote(qrCode, startTime, endTime)));
    }

    /**
     * Sends (or, absent a real SMS provider — see OtpService — returns
     * in dev-mode) the OTP for the given phone.
     */
    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<String>> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        String devModeCode = guestCheckoutService.requestOtp(request.phone());
        String message = devModeCode != null
                ? "OTP (dev-mode, no SMS provider wired): " + devModeCode
                : "OTP sent";
        return ResponseEntity.ok(ApiResponse.ok(message, devModeCode));
    }

    /** Verifies the OTP, creates the booking, and returns a session token (task 13.1/13.2). */
    @PostMapping("/{qrCode}/confirm")
    public ResponseEntity<ApiResponse<GuestCheckoutResponse>> confirm(
            @PathVariable String qrCode, @Valid @RequestBody GuestCheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(guestCheckoutService.confirm(qrCode, request)));
    }
}
