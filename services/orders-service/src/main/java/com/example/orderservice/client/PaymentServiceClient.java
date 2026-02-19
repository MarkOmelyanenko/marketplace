package com.example.orderservice.client;

import com.example.orderservice.dto.CreatePaymentRequest;
import com.example.orderservice.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * HTTP client for payments-service: create payment (with optional idempotency) and internal refund.
 * Refund calls require Ops-Token header.
 */
@Component
public class PaymentServiceClient {

    private static final String OPS_TOKEN_HEADER = "Ops-Token";

    private final WebClient webClient;
    private final String opsToken;

    public PaymentServiceClient(
            @Value("${payment.service.url:http://localhost:8082}") String paymentServiceUrl,
            @Value("${payment.service.ops-token:ops-dev}") String opsToken) {
        this.webClient = WebClient.builder()
            .baseUrl(paymentServiceUrl)
            .build();
        this.opsToken = opsToken != null && !opsToken.isBlank() ? opsToken : "ops-dev";
    }
    
    /**
     * Creates a payment with Buyer-Id header. No idempotency key.
     */
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request, String buyerId) {
        return createPayment(request, buyerId, null);
    }

    /**
     * Creates a payment with Buyer-Id and optional Idempotency-Key. Network call.
     *
     * @param idempotencyKey optional; when set, duplicate requests return same payment
     */
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request, String buyerId, String idempotencyKey) {
        var spec = webClient.post()
            .uri("/v1/payments")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("Buyer-Id", buyerId);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }
        return spec.bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentResponse.class);
    }

    /**
     * Refund a captured payment (internal API). Credits buyer wallet and sets payment to REFUNDED.
     * Requires Ops-Token header for payments-service internal auth.
     */
    public Mono<PaymentResponse> refund(UUID paymentId) {
        return webClient.post()
            .uri("/internal/payments/refund")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(OPS_TOKEN_HEADER, opsToken)
            .bodyValue(java.util.Map.of("paymentId", paymentId.toString()))
            .retrieve()
            .bodyToMono(PaymentResponse.class);
    }
}
