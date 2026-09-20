package com.example.parkio.controller;

import com.example.parkio.dto.request.CashPaymentRequest;
import com.example.parkio.dto.request.PaymentRequest;
import com.example.parkio.dto.request.PhonePushPaymentRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.PaymentResponse;
import com.example.parkio.entity.Staff;
import com.example.parkio.service.PaymentService;
import com.example.parkio.service.StaffService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityUtils securityUtils;
    private final StaffService staffService;

    /** Process payment for a booking */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> pay(
            @Valid @RequestBody PaymentRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        PaymentResponse response = paymentService.processPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /** Get payment for a specific booking */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByBooking(
            @PathVariable Long bookingId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByBooking(bookingId, userId)));
    }

    /** Warden: push a MoMo prompt directly to the driver's phone for a walk-up booking (tasks.md 6.2) */
    @PostMapping("/booking/{bookingId}/phone-push")
    public ResponseEntity<ApiResponse<PaymentResponse>> phonePush(
            @PathVariable Long bookingId,
            @Valid @RequestBody PhonePushPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                paymentService.initiatePhonePush(bookingId, request.phone(), request.momoProvider())));
    }

    /**
     * Warden: log a cash payment when the phone-push prompt fails/declines
     * (tasks.md 6.3). Caller must be active WARDEN/SUPERVISOR/ORG_ADMIN staff
     * of the given organization — resolved here rather than in PaymentService
     * so PaymentService stays free of org/staff concerns.
     */
    @PostMapping("/booking/{bookingId}/cash")
    public ResponseEntity<ApiResponse<PaymentResponse>> logCash(
            @PathVariable Long bookingId,
            @Valid @RequestBody CashPaymentRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        Staff staff = staffService.requireRole(request.organizationId(), userId,
                EnumSet.of(Staff.StaffRole.WARDEN, Staff.StaffRole.SUPERVISOR, Staff.StaffRole.ORG_ADMIN));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentService.logCashPayment(bookingId, staff)));
    }

    /** Get payment by ID (admin only) */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getById(id)));
    }

    /** Issue a refund (admin only) */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(@PathVariable Long id) {
        String actorEmail = securityUtils.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok("Refund issued", paymentService.refund(id, actorEmail)));
    }
}
