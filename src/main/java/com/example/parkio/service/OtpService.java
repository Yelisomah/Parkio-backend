package com.example.parkio.service;

import com.example.parkio.entity.Otp;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.OtpRepository;
import com.example.parkio.sms.SmsGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * OTP request/verify — originally built only for guest checkout (Phase 2,
 * tasks.md 13.2), generalized to also gate phone verification during regular
 * account registration. {@code purpose} keeps a code requested for one from
 * verifying the other, even for the same phone number.
 *
 * SMS delivery goes through {@link SmsGateway} — defaults to a no-op
 * (NoOpSmsGateway) until app.sms.provider is set to a real provider (e.g.
 * "hubtel"). Regardless of whether the SMS actually sends, in dev-mode
 * (default on) the code is ALSO returned directly in the API response, so
 * both flows stay testable even before/without a configured provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsGateway smsGateway;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.otp.ttl-minutes:5}")
    private long ttlMinutes;

    @Value("${app.otp.dev-mode:true}")
    private boolean devMode;

    /**
     * @return the plaintext code ONLY when app.otp.dev-mode=true (for testing without depending on SMS delivery); null otherwise.
     */
    @Transactional
    public String requestOtp(String phone, Otp.Purpose purpose) {
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        Otp otp = Otp.builder()
                .phone(phone)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .build();
        otpRepository.save(otp);

        // Best-effort — an SMS delivery failure shouldn't block issuing the
        // OTP itself (dev-mode below is the fallback either way).
        try {
            SmsGateway.SendResult result = smsGateway.send(phone,
                    "Your Parkio verification code is " + code + ". It expires in " + ttlMinutes + " minutes.");
            if (!result.success()) {
                log.warn("SMS delivery failed for phone {} purpose {}: {}", phone, purpose, result.failureReason());
            }
        } catch (Exception ex) {
            log.error("SMS gateway threw unexpectedly for phone {} purpose {}", phone, purpose, ex);
        }

        // Deliberately never logs the code itself — only that one was issued.
        log.info("OTP issued for phone {} purpose {} (dev-mode={})", phone, purpose, devMode);

        return devMode ? code : null;
    }

    /** @throws ParkioException badRequest if no OTP of this purpose was requested for this phone, it's expired, already used, or doesn't match. */
    @Transactional
    public void verifyOtp(String phone, String code, Otp.Purpose purpose) {
        Otp otp = otpRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> ParkioException.badRequest("No OTP was requested for this phone number"));

        if (otp.isVerified()) {
            throw ParkioException.badRequest("This OTP has already been used — request a new one");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ParkioException.badRequest("OTP expired — request a new one");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            throw ParkioException.badRequest("Incorrect OTP");
        }

        otp.setVerified(true);
        otpRepository.save(otp);
    }
}
