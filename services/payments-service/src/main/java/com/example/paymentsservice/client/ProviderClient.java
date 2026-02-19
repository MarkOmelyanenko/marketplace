package com.example.paymentsservice.client;

import com.example.paymentsservice.dto.ProviderCreatePaymentRequest;
import com.example.paymentsservice.dto.ProviderCreatePaymentResponse;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ProviderClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ProviderClient.class);
    
    private final WebClient webClient;
    private final String providerBaseUrl;
    private final Retry retry;
    
    public ProviderClient(@Value("${provider.base-url}") String providerBaseUrl, Retry retry) {
        this.providerBaseUrl = providerBaseUrl;
        this.retry = retry;
        this.webClient = WebClient.builder()
            .baseUrl(providerBaseUrl)
            .build();
    }
    
    public Mono<ProviderCreatePaymentResponse> createPayment(ProviderCreatePaymentRequest request) {
        logger.info("Calling provider to create payment: paymentId={}, amountCents={}", 
                   request.getPaymentId(), request.getAmountCents());
        
        return webClient.post()
            .uri("/v1/provider/payments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ProviderCreatePaymentResponse.class)
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(response -> logger.info("Provider payment created: providerPaymentId={}", 
                                                 response.getProviderPaymentId()))
            .doOnError(error -> logger.error("Failed to create payment at provider: paymentId={}", 
                                            request.getPaymentId(), error));
    }
}
