package com.example.parkio.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Real Paystack integration.
 *
 * Flow assumed:
 *  1. The frontend collects payment with Paystack Inline/Popup (card or
 *     mobile money channel) and gets back a transaction {@code reference}.
 *  2. That reference is sent to us as {@code gatewayToken} in the
 *     {@code POST /api/v1/payments} request.
 *  3. We verify it server-side against Paystack before ever trusting it
 *     (never trust a client-reported "success").
 *
 * For payment methods that settle asynchronously (e.g. some mobile-money
 * charges Paystack initiates itself), {@link PaymentWebhookController}
 * receives the {@code charge.success} / {@code charge.failed} webhook and
 * finalizes the payment — see {@code ChargeStatus.PENDING} handling in
 * {@code PaymentService}.
 *
 * Active when {@code app.payment.gateway=paystack}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "paystack")
public class PaystackGateway implements PaymentGateway {

    private static final String BASE_URL = "https://api.paystack.co";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaystackGateway(@Value("${app.payment.paystack.secret-key:}") String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("app.payment.gateway=paystack but app.payment.paystack.secret-key is empty — " +
                    "every charge will fail until it is set.");
        }
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public ChargeResult charge(String gatewayToken, BigDecimal amount, String currency, String description) {
        if (gatewayToken == null || gatewayToken.isBlank()) {
            return ChargeResult.failed("Missing Paystack transaction reference");
        }
        try {
            String body = restClient.get()
                    .uri("/transaction/verify/{reference}", gatewayToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            if (!root.path("status").asBoolean(false)) {
                return ChargeResult.failed(root.path("message").asText("Verification failed"));
            }

            JsonNode data = root.path("data");
            String txStatus = data.path("status").asText("");
            String txCurrency = data.path("currency").asText("");
            long amountSubunits = data.path("amount").asLong(-1);
            long expectedSubunits = amount.setScale(2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).longValueExact();

            if (!"success".equals(txStatus)) {
                return switch (txStatus) {
                    case "abandoned", "failed" -> ChargeResult.failed("Paystack transaction " + txStatus);
                    default -> new ChargeResult(ChargeStatus.PENDING, gatewayToken, null);
                };
            }
            if (!currency.equalsIgnoreCase(txCurrency)) {
                log.error("Paystack currency mismatch: expected {} got {} for ref {}", currency, txCurrency, gatewayToken);
                return ChargeResult.failed("Currency mismatch");
            }
            if (amountSubunits != expectedSubunits) {
                log.error("Paystack amount mismatch: expected {} got {} for ref {}", expectedSubunits, amountSubunits, gatewayToken);
                return ChargeResult.failed("Amount mismatch — possible tampering");
            }

            return ChargeResult.success(gatewayToken);

        } catch (RestClientResponseException ex) {
            log.error("Paystack verify call failed: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ChargeResult.failed("Gateway error: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Paystack verify call errored", ex);
            return ChargeResult.failed("Gateway unavailable");
        }
    }

    @Override
    public ChargeResult chargeByPhone(String phone, String momoProvider, BigDecimal amount,
                                      String currency, String description) {
        try {
            String reference = "phonepush_" + java.util.UUID.randomUUID();

            com.fasterxml.jackson.databind.node.ObjectNode momoNode = objectMapper.createObjectNode();
            momoNode.put("phone", phone);
            momoNode.put("provider", momoProvider != null ? momoProvider.toLowerCase() : "mtn");

            com.fasterxml.jackson.databind.node.ObjectNode payloadNode = objectMapper.createObjectNode();
            payloadNode.put("amount", amount.setScale(2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).longValueExact());
            payloadNode.put("email", phone.replaceAll("[^0-9]", "") + "@parkio.guest"); // Paystack requires an email even for momo
            payloadNode.put("currency", currency);
            payloadNode.put("reference", reference);
            payloadNode.set("mobile_money", momoNode);

            String body = restClient.post()
                    .uri("/charge")
                    .body(payloadNode.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            if (!root.path("status").asBoolean(false)) {
                return ChargeResult.failed(root.path("message").asText("Charge initiation failed"));
            }
            String txStatus = root.path("data").path("status").asText("");
            return switch (txStatus) {
                case "success" -> ChargeResult.success(reference);
                case "failed" -> ChargeResult.failed("Mobile money charge failed");
                // "pay_offline" / "send_otp" / anything else — Paystack will call our
                // webhook (charge.success/charge.failed) once the customer responds to the prompt.
                default -> new ChargeResult(ChargeStatus.PENDING, reference, null);
            };
        } catch (Exception ex) {
            log.error("Paystack phone-push charge failed for {}", phone, ex);
            return ChargeResult.failed("Gateway unavailable");
        }
    }

    @Override
    public ChargeResult refund(String gatewayReference, BigDecimal amount) {
        try {
            String payload = objectMapper.createObjectNode()
                    .put("transaction", gatewayReference)
                    .put("amount", amount.setScale(2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).longValueExact())
                    .toString();

            String body = restClient.post()
                    .uri("/refund")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            if (!root.path("status").asBoolean(false)) {
                return ChargeResult.failed(root.path("message").asText("Refund failed"));
            }
            String refId = root.path("data").path("id").asText(gatewayReference);
            return ChargeResult.success(refId);

        } catch (Exception ex) {
            log.error("Paystack refund call failed for {}", gatewayReference, ex);
            return ChargeResult.failed("Refund gateway error");
        }
    }
}
