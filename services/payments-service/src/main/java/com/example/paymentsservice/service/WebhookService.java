package com.example.paymentsservice.service;

import com.example.paymentsservice.dto.ProviderWebhookRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifies provider webhook HMAC-SHA256 signature and delegates processing to {@link PaymentService}.
 */
@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String webhookSecret;
    private final PaymentService paymentService;

    public WebhookService(@Value("${provider.webhook-secret}") String webhookSecret,
                         PaymentService paymentService) {
        this.webhookSecret = webhookSecret;
        this.paymentService = paymentService;
    }

    /**
     * Verifies HMAC-SHA256 of raw body against provided signature. Uses constant-time comparison.
     *
     * @param rawBody           exact request body (must match what provider signed)
     * @param providedSignature X-Provider-Signature header value
     * @return true if signature is valid
     */
    public boolean verifySignature(String rawBody, String providedSignature) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String computedSignature = hexString.toString();
            return MessageDigest.isEqual(
                computedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8),
                providedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    public void processWebhook(ProviderWebhookRequest request, String providerEventId) {
        paymentService.handleWebhook(request, providerEventId);
    }
}
