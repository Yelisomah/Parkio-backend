package com.example.parkio.dto.response;

/**
 * Returned once OTP verification succeeds and the booking is created.
 * accessToken lets the guest's client immediately call the normal
 * authenticated endpoints (pay, view booking, etc.) — OTP verification IS
 * the login event here, there's no separate password step for a guest.
 */
public record GuestCheckoutResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        BookingResponse booking
) {
    public GuestCheckoutResponse(String accessToken, long expiresIn, BookingResponse booking) {
        this(accessToken, "Bearer", expiresIn, booking);
    }
}
