package com.example.offerservice.service;

import com.example.offerservice.dto.ApplyAiRequest;
import com.example.offerservice.dto.CatalogOfferResponse;
import com.example.offerservice.dto.CreateOfferRequest;
import com.example.offerservice.dto.OfferResponse;
import com.example.offerservice.dto.UpdateOfferRequest;
import com.example.offerservice.entity.IdempotencyKey;
import com.example.offerservice.entity.Offer;
import com.example.offerservice.event.OfferCreatedEvent;
import com.example.offerservice.repository.IdempotencyKeyRepository;
import com.example.offerservice.repository.OfferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Offer lifecycle: create (with outbox event), list, get, update, apply AI, publish (listing fee),
 * and Kafka-driven status updates (enriched, payment captured/failed). All mutations transactional.
 */
@Service
public class OfferService {

    private static final Logger logger = LoggerFactory.getLogger(OfferService.class);
    private static final String OFFER_CREATED_TOPIC = "offer.created";
    
    private final OfferRepository offerRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final com.example.offerservice.client.PaymentServiceClient paymentServiceClient;
    private final OutboxService outboxService;
    private final com.example.offerservice.metrics.OfferMetrics offerMetrics;
    
    public OfferService(OfferRepository offerRepository, IdempotencyKeyRepository idempotencyKeyRepository,
                       KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper,
                       com.example.offerservice.client.PaymentServiceClient paymentServiceClient,
                       OutboxService outboxService,
                       com.example.offerservice.metrics.OfferMetrics offerMetrics) {
        this.offerRepository = offerRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.paymentServiceClient = paymentServiceClient;
        this.outboxService = outboxService;
        this.offerMetrics = offerMetrics;
    }
    
    /**
     * Looks up stored idempotency key for create-offer. Read-only.
     */
    public Optional<IdempotencyKey> findIdempotencyKey(String idempotencyKey, String partnerId) {
        return idempotencyKeyRepository.findByIdempotencyKeyAndPartnerId(idempotencyKey, partnerId);
    }

    /**
     * Creates an offer (status ENRICHING), optionally stores idempotency key, and publishes
     * offer.created to outbox. Transactional. Event publish failure does not roll back.
     *
     * @param request       title, description, optional priceCents/currency
     * @param partnerId     authenticated partner
     * @param idempotencyKey optional; when set, duplicate creates return same offer
     * @return created offer response
     */
    @Transactional
    public OfferResponse createOffer(CreateOfferRequest request, String partnerId, String idempotencyKey) {
        Offer offer = new Offer();
        offer.setPartnerId(partnerId);
        offer.setStatus("ENRICHING");
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setPriceCents(request.getPriceCents() != null ? request.getPriceCents() : 499);
        offer.setCurrency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency() : "USD");

        Offer savedOffer = offerRepository.save(offer);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey key = new IdempotencyKey();
            key.setIdempotencyKey(idempotencyKey);
            key.setPartnerId(partnerId);
            key.setOfferId(savedOffer.getId());
            idempotencyKeyRepository.save(key);
        }
        publishOfferCreatedEvent(savedOffer);
        offerMetrics.incrementOffersCreated();
        return toResponse(savedOffer);
    }

    private void publishOfferCreatedEvent(Offer offer) {
        try {
            OfferCreatedEvent event = new OfferCreatedEvent(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                offer.getId(),
                offer.getPartnerId(),
                offer.getTitle(),
                offer.getDescription()
            );
            outboxService.publishEvent("OFFER", offer.getId(), OFFER_CREATED_TOPIC, event);
            logger.info("Queued offer.created event to outbox for offerId: {}", offer.getId());
        } catch (Exception e) {
            logger.error("Failed to queue offer.created event to outbox for offerId: {}", offer.getId(), e);
        }
    }
    
    public List<OfferResponse> getOffersByPartner(String partnerId) {
        List<Offer> offers = offerRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
        return offers.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    public OfferResponse getOfferById(UUID id, String partnerId) {
        Offer offer = offerRepository.findByIdAndPartnerId(id, partnerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));
        return toResponse(offer);
    }

    public List<CatalogOfferResponse> getPublishedOffers() {
        List<Offer> offers = offerRepository.findTop50ByStatusOrderByCreatedAtDesc("PUBLISHED");
        return offers.stream()
            .map(this::toCatalogResponse)
            .collect(Collectors.toList());
    }
    
    public Optional<Offer> findPublishedOfferById(UUID id) {
        Optional<Offer> offerOpt = offerRepository.findById(id);
        if (offerOpt.isPresent() && "PUBLISHED".equals(offerOpt.get().getStatus())) {
            return offerOpt;
        }
        return Optional.empty();
    }
    
    @Transactional
    public OfferResponse updateOffer(UUID id, UpdateOfferRequest request, String partnerId) {
        Offer offer = offerRepository.findByIdAndPartnerId(id, partnerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));
        
        if (request.getTitle() != null) {
            offer.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            offer.setDescription(request.getDescription());
        }
        if (request.getPriceCents() != null) {
            offer.setPriceCents(request.getPriceCents());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            offer.setCurrency(request.getCurrency());
        }

        Offer updatedOffer = offerRepository.save(offer);
        return toResponse(updatedOffer);
    }
    
    @Transactional
    public OfferResponse applyAi(UUID id, ApplyAiRequest request, String partnerId) {
        Offer offer = offerRepository.findByIdAndPartnerId(id, partnerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));
        
        if (Boolean.TRUE.equals(request.getUseTitle()) && offer.getAiTitle() != null) {
            offer.setTitle(offer.getAiTitle());
        }
        
        if (Boolean.TRUE.equals(request.getUseDescription()) && offer.getAiDescription() != null) {
            offer.setDescription(offer.getAiDescription());
        }
        
        Offer updatedOffer = offerRepository.save(offer);
        return toResponse(updatedOffer);
    }
    
    /**
     * Kafka consumer handler: applies enrichment (AI title/description/tags) and sets status READY.
     * Idempotent: if already READY with AI fields set, ignores. Transactional.
     */
    @Transactional
    public void handleOfferEnriched(UUID offerId, String partnerId, String aiTitle, String aiDescription, List<String> aiTags) {
        Optional<Offer> offerOpt = offerRepository.findByIdAndPartnerId(offerId, partnerId);
        if (offerOpt.isEmpty()) {
            logger.warn("Offer not found for enrichment: offerId={}, partnerId={}", offerId, partnerId);
            return;
        }
        Offer offer = offerOpt.get();
        if (!offer.getPartnerId().equals(partnerId)) {
            logger.warn("Partner mismatch for enrichment: offerId={}, expectedPartnerId={}, actualPartnerId={}",
                       offerId, offer.getPartnerId(), partnerId);
            return;
        }
        if ("READY".equals(offer.getStatus()) && offer.getAiTitle() != null && offer.getAiDescription() != null) {
            logger.info("Offer already enriched, ignoring duplicate event: offerId={}", offerId);
            return;
        }
        offer.setAiTitle(aiTitle);
        offer.setAiDescription(aiDescription);
        if (aiTags != null && !aiTags.isEmpty()) {
            try {
                offer.setAiTags(objectMapper.writeValueAsString(aiTags));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize aiTags for offerId: {}", offerId, e);
                offer.setAiTags("[]");
            }
        }
        offer.setStatus("READY");
        
        offerRepository.save(offer);
        logger.info("Applied enrichment to offer: offerId={}", offerId);
        offerMetrics.incrementOffersEnriched();
    }

    /**
     * Starts publish flow: sets PUBLISH_PENDING and creates listing fee payment. Idempotent if already PUBLISHED.
     * Uses a unique idempotency key per attempt so a previous FAILED payment does not return cached result.
     * Transactional.
     */
    @Transactional
    public void publishOffer(UUID offerId, String partnerId) {
        Offer offer = offerRepository.findByIdAndPartnerId(offerId, partnerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));
        if ("PUBLISHED".equals(offer.getStatus())) {
            logger.info("Offer already published: offerId={}", offerId);
            return;
        }
        String idempotencyKey = "publish-" + offerId + "-" + System.currentTimeMillis();
        offer.setStatus("PUBLISH_PENDING");
        offerRepository.save(offer);
        paymentServiceClient.createListingFeePayment(offerId, partnerId, idempotencyKey);
        logger.info("Initiated publish flow for offer: offerId={}, idempotencyKey={}", offerId, idempotencyKey);
    }

    /**
     * Kafka consumer handler: sets offer to PUBLISHED when listing fee payment is captured.
     * Only updates if status is PUBLISH_PENDING. Transactional.
     */
    @Transactional
    public void handlePaymentCaptured(UUID offerId, String partnerId) {
        Optional<Offer> offerOpt = offerRepository.findByIdAndPartnerId(offerId, partnerId);
        if (offerOpt.isEmpty()) {
            logger.warn("Offer not found for payment captured: offerId={}, partnerId={}", offerId, partnerId);
            return;
        }
        Offer offer = offerOpt.get();
        if ("PUBLISH_PENDING".equals(offer.getStatus())) {
            offer.setStatus("PUBLISHED");
            offerRepository.save(offer);
            logger.info("Offer published after payment captured: offerId={}", offerId);
        }
    }
    
    @Transactional
    public void deleteOffer(UUID id, String partnerId) {
        Offer offer = offerRepository.findByIdAndPartnerId(id, partnerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));
        offerRepository.delete(offer);
        logger.info("Offer deleted: offerId={}, partnerId={}", id, partnerId);
    }

    /**
     * Kafka consumer handler: sets offer to PUBLISH_FAILED when listing fee payment fails.
     * Only updates if status is PUBLISH_PENDING. Transactional.
     */
    @Transactional
    public void handlePaymentFailed(UUID offerId, String partnerId) {
        Optional<Offer> offerOpt = offerRepository.findByIdAndPartnerId(offerId, partnerId);
        if (offerOpt.isEmpty()) {
            logger.warn("Offer not found for payment failed: offerId={}, partnerId={}", offerId, partnerId);
            return;
        }
        Offer offer = offerOpt.get();
        if ("PUBLISH_PENDING".equals(offer.getStatus())) {
            offer.setStatus("PUBLISH_FAILED");
            offerRepository.save(offer);
            logger.info("Offer publish failed after payment failed: offerId={}", offerId);
        }
    }
    
    private OfferResponse toResponse(Offer offer) {
        OfferResponse response = new OfferResponse(
            offer.getId(),
            offer.getPartnerId(),
            offer.getStatus(),
            offer.getTitle(),
            offer.getDescription(),
            offer.getCreatedAt(),
            offer.getUpdatedAt()
        );
        response.setPriceCents(offer.getPriceCents() != null ? offer.getPriceCents() : 499);
        response.setCurrency(offer.getCurrency() != null && !offer.getCurrency().isBlank() ? offer.getCurrency() : "USD");
        response.setAiTitle(offer.getAiTitle());
        response.setAiDescription(offer.getAiDescription());
        if (offer.getAiTags() != null && !offer.getAiTags().isEmpty()) {
            try {
                List<String> tags = Arrays.asList(objectMapper.readValue(offer.getAiTags(), String[].class));
                response.setAiTags(tags);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse aiTags for offerId: {}", offer.getId(), e);
                response.setAiTags(List.of());
            }
        }
        
        return response;
    }
    
    private CatalogOfferResponse toCatalogResponse(Offer offer) {
        List<String> tags = List.of();
        if (offer.getAiTags() != null && !offer.getAiTags().isEmpty()) {
            try {
                tags = Arrays.asList(objectMapper.readValue(offer.getAiTags(), String[].class));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse aiTags for offerId: {}", offer.getId(), e);
            }
        }
        
        return new CatalogOfferResponse(
            offer.getId(),
            offer.getPartnerId(),
            offer.getTitle(),
            offer.getDescription(),
            offer.getPriceCents() != null ? offer.getPriceCents() : 499,
            offer.getCurrency() != null && !offer.getCurrency().isBlank() ? offer.getCurrency() : "USD",
            tags,
            offer.getCreatedAt(),
            offer.getUpdatedAt()
        );
    }
}
