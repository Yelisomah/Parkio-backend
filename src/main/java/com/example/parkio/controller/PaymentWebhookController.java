package com.example.parkio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.parkio.service.PaymentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Receives asynchronous payment confirmations from the payment gateway.
 * MUST be public (no JWT) — the gateway, not a logged-in user, calls this —
 * see the permitAll() rule in SecurityConfig for this path. Authenticity is
 * instead verified via the HMAC signature header on every request.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.payment.paystack.secret-key:}")
    private String paystackSecretKey;

    @PostMapping("/paystack")
    public ResponseEntity<String> paystackWebhook(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (signature == null || !isValidSignature(rawBody, signature)) {
            log.warn("Rejected Paystack webhook with invalid/missing signature");
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode event = objectMapper.readTree(rawBody);
            String eventType = event.path("event").asText("");
            JsonNode data = event.path("data");
            String reference = data.path("reference").asText(null);

            if (reference == null) {
                return ResponseEntity.ok("ignored");
            }

            switch (eventType) {
                case "charge.success" -> paymentService.finalizeFromWebhook(reference, true, null);
                case "charge.failed" -> paymentService.finalizeFromWebhook(reference, false,
                        data.path("gateway_response").asText("Charge failed"));
                default -> log.debug("Unhandled Paystack event type: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process Paystack webhook", ex);
            // Still return 200 so Paystack doesn't retry indefinitely on a
            // permanent parsing bug — the payment simply stays PROCESSING and
            // is reconciled manually/by a follow-up poll.
        }

        return ResponseEntity.ok("received");
    }

    private boolean isValidSignature(String rawBody, String signature) {
        if (paystackSecretKey == null || paystackSecretKey.isBlank()) {
            log.error("Cannot verify Paystack webhook — app.payment.paystack.secret-key is not configured");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(paystackSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Error computing webhook signature", ex);
            return false;
        }
    }
}
