package com.example.parkio.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Hubtel SMS ("Quick Send") integration — the standard path for SMS delivery
 * in Ghana without negotiating separate agreements with each telco (MTN,
 * Telecel/Vodafone, AirtelTigo). Built from Hubtel's own current docs:
 *   - https://businessdocs-developers.hubtel.com/docs/simple-messaging
 *   - https://developers.hubtel.com/docs/getting-started-with-sms
 *   - https://news.hubtel.com/faqs-sms-api-management/ (confirms the base
 *     URL migrated from api.hubtel.com/api.smsgh.com to smsc.hubtel.com —
 *     "all other details remain the same")
 *
 * Not verified end-to-end against a live Hubtel account in this session (no
 * credentials available here) — the request/response shape below matches
 * Hubtel's published sample exactly, but confirm against a real sandbox
 * send before relying on it.
 *
 * Active when {@code app.sms.provider=hubtel}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "hubtel")
public class HubtelSmsGateway implements SmsGateway {

    private static final String BASE_URL = "https://smsc.hubtel.com/v1/messages/send";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;
    private final String senderId;

    public HubtelSmsGateway(@Value("${app.sms.hubtel.client-id:}") String clientId,
                            @Value("${app.sms.hubtel.client-secret:}") String clientSecret,
                            @Value("${app.sms.hubtel.sender-id:Parkio}") String senderId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.senderId = senderId;

        if (clientId.isBlank() || clientSecret.isBlank()) {
            log.warn("app.sms.provider=hubtel but client-id/client-secret are empty — every send will fail until they're set.");
        }

        String basicAuth = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Basic " + basicAuth)
                .build();
    }

    @Override
    public SendResult send(String toPhone, String message) {
        try {
            // Quick Send (GET) — clientid/clientsecret are passed both as query
            // params AND via the Basic Auth header, matching Hubtel's own sample
            // curl exactly (redundant-looking, but that's what their docs show).
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("clientid", clientId)
                            .queryParam("clientsecret", clientSecret)
                            .queryParam("from", senderId)
                            .queryParam("to", toPhone)
                            .queryParam("content", message)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            int status = root.path("status").asInt(-1);
            String messageId = root.path("messageId").asText(null);
            String statusDescription = root.path("statusDescription").asText("");

            // Per Hubtel's docs: status 0 = "request submitted successfully".
            if (status == 0) {
                return SendResult.success(messageId);
            }
            return SendResult.failed("Hubtel status " + status + ": " + statusDescription);

        } catch (RestClientResponseException ex) {
            log.error("Hubtel SMS send failed: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return SendResult.failed("Gateway error: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Hubtel SMS send errored for {}", toPhone, ex);
            return SendResult.failed("Gateway unavailable");
        }
    }
}
