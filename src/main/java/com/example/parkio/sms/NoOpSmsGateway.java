package com.example.parkio.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Does nothing but log — the safe default until {@code app.sms.provider} is
 * set to a real provider. This is exactly the state the codebase was in
 * before this feature existed: OTPs get generated and stored, just never
 * actually texted anywhere (dev-mode returning the code in the API response
 * is what makes the flow testable in the meantime).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "none", matchIfMissing = true)
public class NoOpSmsGateway implements SmsGateway {

    @Override
    public SendResult send(String toPhone, String message) {
        log.info("SMS gateway is 'none' — not actually sending to {} ({} chars)", toPhone, message.length());
        return SendResult.failed("No SMS provider configured (app.sms.provider=none)");
    }
}
