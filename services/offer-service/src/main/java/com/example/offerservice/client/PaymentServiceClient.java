package com.example.offerservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceClient.class);
    
    private final WebClient webClient;
    private final String paymentServiceUrl;
    
    public PaymentServiceClient(@Value("${payment.service.url:http://localhost:8082}") String paymentServiceUrl) {
        this.paymentServiceUrl = paymentServiceUrl;
        this.webClient = WebClient.builder()
            .baseUrl(paymentServiceUrl)
            .build();
    }
    
    public void createListingFeePayment(UUID offerId, String partnerId) {
        createListingFeePayment(offerId, partnerId, "publish-" + offerId.toString());
    }

    public void createListingFeePayment(UUID offerId, String partnerId, String idempotencyKey) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("offerId", offerId.toString());
            body.put("amountCents", 199); // $1.99 USD listing fee
            body.put("currency", "USD");
            
            webClient.post()
                .uri("/v1/payments")
                .header("Partner-Id", partnerId)
                .header("Idempotency-Key", idempotencyKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> logger.info("Created listing fee payment for offerId: {}", offerId),
                    error -> logger.error("Failed to create listing fee payment for offerId: {}", offerId, error)
                );
        } catch (Exception e) {
            logger.error("Error creating listing fee payment for offerId: {}", offerId, e);
        }
    }
}
