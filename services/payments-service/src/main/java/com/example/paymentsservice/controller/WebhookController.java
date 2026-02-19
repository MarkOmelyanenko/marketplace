package com.example.paymentsservice.controller;

import com.example.paymentsservice.dto.ProviderWebhookRequest;
import com.example.paymentsservice.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives provider payment webhooks. Expects raw body in request attribute "rawBody"
 * (set by {@link com.example.paymentsservice.filter.RawBodyFilter}) for signature verification.
 */
@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/provider")
    public ResponseEntity<Map<String, String>> handleProviderWebhook(
            @RequestBody ProviderWebhookRequest request,
            @RequestHeader("X-Provider-Event-Id") String providerEventId,
            @RequestHeader("X-Provider-Signature") String signature,
            HttpServletRequest httpRequest) {
        String rawBody = (String) httpRequest.getAttribute("rawBody");
        if (rawBody == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Missing request body"));
        }
        if (!webhookService.verifySignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid signature"));
        }
        webhookService.processWebhook(request, providerEventId);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }
}
