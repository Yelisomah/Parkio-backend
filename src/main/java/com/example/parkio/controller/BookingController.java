package com.example.parkio.controller;

import com.example.parkio.dto.request.BookingExtensionRequest;
import com.example.parkio.dto.request.BookingRequest;
import com.example.parkio.dto.request.WalkUpBookingRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.BookingResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.service.BookingService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final SecurityUtils securityUtils;

    /** Get current user's bookings */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getByUser(userId, pageable)));
    }

    /** Get a single booking (owner or admin) */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.isAdmin();
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getById(id, userId, isAdmin)));
    }

    /** Look up by booking reference (owner or admin) */
    @GetMapping("/ref/{reference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getByReference(
            @PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getByReference(reference)));
    }

    /** Create a new booking */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(
            @Valid @RequestBody BookingRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        BookingResponse response = bookingService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /** Check in (user) */
    @PatchMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<BookingResponse>> checkIn(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Checked in", bookingService.checkIn(id, userId)));
    }

    /** Check out (user) */
    @PatchMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<BookingResponse>> checkOut(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Checked out", bookingService.checkOut(id, userId)));
    }

    /** Cancel booking (owner or admin) */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.isAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled",
                bookingService.cancel(id, userId, isAdmin)));
    }

    /** Extend an in-progress/confirmed booking's end time (task 5.4) */
    @PostMapping("/{id}/extend")
    public ResponseEntity<ApiResponse<BookingResponse>> extend(
            @PathVariable Long id, @Valid @RequestBody BookingExtensionRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(bookingService.extend(id, userId, request)));
    }

    /** Warden walk-up booking (tasks.md task 6) — caller must be WARDEN/SUPERVISOR/ORG_ADMIN staff of the given organization. */
    @PostMapping("/walk-up")
    public ResponseEntity<ApiResponse<BookingResponse>> createWalkUp(
            @Valid @RequestBody WalkUpBookingRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(bookingService.createWalkUp(userId, request)));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    /** Admin: confirm a pending booking */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Booking confirmed", bookingService.confirm(id)));
    }
}
