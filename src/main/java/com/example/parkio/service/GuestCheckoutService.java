package com.example.parkio.service;

import com.example.parkio.dto.request.GuestCheckoutRequest;
import com.example.parkio.dto.response.BookingResponse;
import com.example.parkio.dto.response.GuestCheckoutQuoteResponse;
import com.example.parkio.dto.response.GuestCheckoutResponse;
import com.example.parkio.entity.Otp;
import com.example.parkio.entity.ParkingLot;
import com.example.parkio.entity.SpaceQrCode;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.SpaceQrCodeRepository;
import com.example.parkio.security.JwtUtil;
import com.example.parkio.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * QR-signage guest walk-up checkout (Phase 2, tasks.md 13.1/13.2) — built now
 * ahead of the MVP-first sequencing tech-standards.md states, at explicit
 * request. See STATUS_AND_ROADMAP.md for that call-out.
 *
 * Flow: scan permanent space QR → getQuote (no side effects) → requestOtp →
 * confirm (verifies OTP, creates the booking, auto-issues a real session
 * token so the guest's client can immediately use every other authenticated
 * endpoint — payment included — with no separate login step).
 */
@Service
@RequiredArgsConstructor
public class GuestCheckoutService {

    private final SpaceQrCodeRepository spaceQrCodeRepository;
    private final ParkingLotService parkingLotService;
    private final ParkingSpotService parkingSpotService;
    private final PricingService pricingService;
    private final BookingService bookingService;
    private final OtpService otpService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public GuestCheckoutQuoteResponse getQuote(String qrCode, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        ParkingLot space = resolveSpace(qrCode);

        return parkingSpotService.findAnyAvailable(space.getId(), start, end)
                .map(spot -> new GuestCheckoutQuoteResponse(
                        space.getId(), space.getName(), true,
                        pricingService.calculateAmount(spot, start, end), start, end))
                .orElseGet(() -> new GuestCheckoutQuoteResponse(
                        space.getId(), space.getName(), false,
                        estimateFromLotRate(space, start, end), start, end));
    }

    /** Rough estimate only, used when nothing is actually available to price precisely against. */
    private BigDecimal estimateFromLotRate(ParkingLot space, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        long hours = Math.max(1, Duration.between(start, end).toMinutes() / 60);
        return space.getHourlyRate().multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
    }

    public String requestOtp(String phone) {
        return otpService.requestOtp(phone, Otp.Purpose.GUEST_CHECKOUT);
    }

    @Transactional
    public GuestCheckoutResponse confirm(String qrCode, GuestCheckoutRequest request) {
        otpService.verifyOtp(request.driverPhone(), request.otpCode(), Otp.Purpose.GUEST_CHECKOUT);

        ParkingLot space = resolveSpace(qrCode);
        BookingResponse booking = bookingService.createGuestQrBooking(space.getId(), request);

        // OTP verification IS the login event for a guest — issue a real
        // session token so their client can call payment/etc. normally from here.
        UserDetails userDetails = userDetailsService.loadUserByUsername(booking.userEmail());
        String accessToken = jwtUtil.generateAccessToken(userDetails);

        return new GuestCheckoutResponse(accessToken, jwtUtil.getExpirationMillis(), booking);
    }

    private ParkingLot resolveSpace(String qrCode) {
        SpaceQrCode qr = spaceQrCodeRepository.findByCode(qrCode)
                .orElseThrow(() -> ParkioException.notFound("Unrecognized QR code"));
        return parkingLotService.findById(qr.getSpace().getId());
    }
}
