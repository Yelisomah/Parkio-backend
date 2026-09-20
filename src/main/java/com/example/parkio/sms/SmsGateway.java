package com.example.parkio.sms;

/**
 * SMS delivery, abstracted the same way {@code PaymentGateway} abstracts
 * payment providers — so OtpService (or anything else that needs to text
 * someone) doesn't care which provider is behind it.
 *
 * {@code app.sms.provider} selects the implementation:
 *  - "none" (default) → NoOpSmsGateway — logs only, sends nothing. Safe
 *    default until a real provider is configured.
 *  - "hubtel" → HubtelSmsGateway
 */
public interface SmsGateway {

    SendResult send(String toPhone, String message);

    record SendResult(boolean success, String providerMessageId, String failureReason) {
        public static SendResult success(String providerMessageId) {
            return new SendResult(true, providerMessageId, null);
        }
        public static SendResult failed(String reason) {
            return new SendResult(false, null, reason);
        }
    }
}
