package com.example.providersimulator.service;

import com.example.providersimulator.dto.CreatePaymentRequest;
import com.example.providersimulator.dto.CreatePaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Simulates payment provider: creates payment with deterministic CAPTURED/FAILED (amountCents % 10 == 0 fails)
 * and sends async webhook to payments-service.
 */
@Service
public class ProviderService {

    private static final Logger logger = LoggerFactory.getLogger(ProviderService.class);

    private final WebhookService webhookService;

    public ProviderService(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Creates a simulated payment and triggers async webhook. Deterministic: amountCents divisible by 10 → FAILED.
     *
     * @param request payment details from payments-service
     * @return response with provider payment id
     */
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        String providerPaymentId = "psp_" + UUID.randomUUID().toString();
        logger.info("Created payment at provider: providerPaymentId={}, paymentId={}, amountCents={}",
                   providerPaymentId, request.getPaymentId(), request.getAmountCents());
        String status = (request.getAmountCents() % 10 == 0) ? "FAILED" : "CAPTURED";
        OffsetDateTime occurredAt = OffsetDateTime.now();
        webhookService.sendWebhook(
            providerPaymentId,
            status,
            request.getAmountCents(),
            request.getCurrency(),
            occurredAt
        );
        
        return new CreatePaymentResponse(providerPaymentId);
    }
}
