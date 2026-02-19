package com.example.providersimulator.service;

import com.example.providersimulator.dto.WebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Sends CAPTURED/FAILED webhooks to payments-service with HMAC-SHA256 signature.
 * Async; uses same JSON body for signing and sending to avoid serialization mismatch.
 */
@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String webhookUrl;
    private final String webhookSecret;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WebhookService(@Value("${payments.webhook-url}") String webhookUrl,
                         @Value("${payments.webhook-secret}") String webhookSecret,
                         ObjectMapper objectMapper) {
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
        this.webClient = WebClient.builder().build();
        this.objectMapper = objectMapper;
    }

    /**
     * Sends webhook asynchronously after 1–3s delay. Signs body with HMAC-SHA256.
     *
     * @param providerPaymentId provider's payment id
     * @param status            CAPTURED or FAILED
     * @param amountCents       amount
     * @param currency          currency code
     * @param occurredAt        event time
     */
    @Async
    public void sendWebhook(String providerPaymentId, String status, Integer amountCents,
                           String currency, OffsetDateTime occurredAt) {
        try {
            Thread.sleep(1000 + (long)(Math.random() * 2000));
            WebhookPayload payload = new WebhookPayload(
                providerPaymentId,
                status,
                amountCents,
                currency,
                occurredAt
            );
            String jsonBody = objectMapper.writeValueAsString(payload);
            String signature = computeSignature(jsonBody);
            String eventId = UUID.randomUUID().toString();
            webClient.post()
                .uri(webhookUrl)
                .header("X-Provider-Event-Id", eventId)
                .header("X-Provider-Signature", signature)
                .header("Content-Type", "application/json")
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> logger.info("Webhook sent successfully: eventId={}, providerPaymentId={}, status={}", 
                                          eventId, providerPaymentId, status),
                    error -> logger.error("Failed to send webhook: eventId={}, providerPaymentId={}", 
                                         eventId, providerPaymentId, error)
                );
        } catch (Exception e) {
            logger.error("Error sending webhook: providerPaymentId={}", providerPaymentId, e);
        }
    }
    
    private String computeSignature(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Error computing signature", e);
            throw new RuntimeException("Failed to compute signature", e);
        }
    }
}
