package com.example.parkio.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulated gateway — always succeeds.
 *
 * Active when {@code app.payment.gateway=simulated} (the default).
 * Replace with a real implementation (e.g. {@code PaystackGateway})
 * and set {@code app.payment.gateway=paystack} in production.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "simulated", matchIfMissing = true)
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public ChargeResult charge(String gatewayToken, BigDecimal amount,
                               String currency, String description) {
        String ref = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        log.info("[SimulatedGateway] Charged {} {} — ref={} token={}", amount, currency, ref, gatewayToken);
        return ChargeResult.success(ref);
    }

    @Override
    public ChargeResult refund(String gatewayReference, BigDecimal amount) {
        String ref = "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        log.info("[SimulatedGateway] Refunded {} — original={} new={}", amount, gatewayReference, ref);
        return ChargeResult.success(ref);
    }
}
