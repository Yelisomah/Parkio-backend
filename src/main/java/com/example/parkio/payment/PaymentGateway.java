package com.example.parkio.payment;

import java.math.BigDecimal;

/**
 * Pluggable payment gateway contract.
 *
 * Swap the active implementation in Spring config to integrate
 * a real provider (Stripe, Paystack, Flutterwave …).
 */
public interface PaymentGateway {

    /**
     * Attempt to charge the given amount using the provider's token/nonce.
     *
     * @param gatewayToken  provider-specific token from the frontend (card nonce, etc.)
     * @param amount        exact amount to charge
     * @param currency      ISO-4217 currency code, e.g. "GHS", "USD"
     * @param description   human-readable charge description
     * @return              result carrying status and provider's reference id
     */
    ChargeResult charge(String gatewayToken, BigDecimal amount,
                        String currency, String description);

    /**
     * Push a mobile-money charge prompt directly to a phone number — no
     * client-side token needed (Kiro design.md's warden walk-up flow: "Payment
     * module sends a MoMo prompt directly to the phone number, no account
     * required"). Async by nature: typically returns PENDING, finalized later
     * via webhook (see PaymentWebhookController / PaymentService.finalizeFromWebhook).
     *
     * Default throws — only gateways that support a phone-push flow (e.g.
     * Paystack's Charge API with a mobile_money channel) need to override it.
     */
    default ChargeResult chargeByPhone(String phone, String momoProvider, BigDecimal amount,
                                       String currency, String description) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support phone-push charges");
    }

    /**
     * Refund a previously completed charge.
     *
     * @param gatewayReference  the reference returned by {@link #charge}
     * @param amount            amount to refund (may be partial)
     * @return                  result carrying updated status
     */
    ChargeResult refund(String gatewayReference, BigDecimal amount);

    // ── Value objects ─────────────────────────────────────────────────────────

    enum ChargeStatus { SUCCESS, FAILED, PENDING }

    record ChargeResult(
            ChargeStatus status,
            String gatewayReference,   // provider's transaction / charge ID
            String failureReason       // null on success
    ) {
        public static ChargeResult success(String ref) {
            return new ChargeResult(ChargeStatus.SUCCESS, ref, null);
        }
        public static ChargeResult failed(String reason) {
            return new ChargeResult(ChargeStatus.FAILED, null, reason);
        }
    }
}
