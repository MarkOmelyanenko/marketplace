package com.example.orderservice.client;

import com.example.orderservice.dto.CatalogOfferResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * HTTP client for offer-service catalog. Fetches published offers (list filtered by id).
 */
@Component
public class OfferServiceClient {

    private final WebClient webClient;

    public OfferServiceClient(@Value("${offer.service.url:http://localhost:8081}") String offerServiceUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(offerServiceUrl)
            .build();
    }

    /**
     * Returns a published offer by id. Uses catalog list endpoint and filters by id. Network call.
     *
     * @param offerId offer id
     * @return offer if published and found, else error
     */
    public Mono<CatalogOfferResponse> getPublishedOffer(UUID offerId) {
        return webClient.get()
            .uri("/v1/catalog/offers")
            .retrieve()
            .bodyToFlux(CatalogOfferResponse.class)
            .filter(offer -> offer.getId().equals(offerId))
            .next()
            .switchIfEmpty(Mono.error(new RuntimeException("Offer not found or not published")));
    }
}
