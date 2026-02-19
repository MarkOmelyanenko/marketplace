package com.example.enrichmentservice.consumer;

import com.example.enrichmentservice.event.OfferCreatedEvent;
import com.example.enrichmentservice.event.OfferEnrichedEvent;
import com.example.enrichmentservice.service.AiEnrichmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class OfferCreatedConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(OfferCreatedConsumer.class);
    private static final String OFFER_ENRICHED_TOPIC = "offer.enriched";
    
    private final AiEnrichmentService aiEnrichmentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public OfferCreatedConsumer(AiEnrichmentService aiEnrichmentService,
                               KafkaTemplate<String, Object> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.aiEnrichmentService = aiEnrichmentService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "offer.created", groupId = "enrichment-service")
    public void consume(String payload) {
        logger.info("Received raw offer.created message: {}", payload);
        try {
            OfferCreatedEvent event = objectMapper.readValue(payload, OfferCreatedEvent.class);
            logger.info("Parsed offer.created event: eventId={}, offerId={}, partnerId={}", 
                       event.getEventId(), event.getOfferId(), event.getPartnerId());
            
            AiEnrichmentService.EnrichmentResult enrichment = aiEnrichmentService.generateEnrichment(
                event.getTitle(),
                event.getDescription()
            );
            
            OfferEnrichedEvent enrichedEvent = new OfferEnrichedEvent(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                event.getOfferId(),
                event.getPartnerId(),
                enrichment.getAiTitle(),
                enrichment.getAiDescription(),
                enrichment.getAiTags()
            );
            
            kafkaTemplate.send(OFFER_ENRICHED_TOPIC, enrichedEvent.getOfferId().toString(), enrichedEvent);
            logger.info("Published offer.enriched event: eventId={}, offerId={}", 
                       enrichedEvent.getEventId(), enrichedEvent.getOfferId());
            
        } catch (Exception e) {
            logger.error("Error processing offer.created event: payload={}", payload, e);
            throw new RuntimeException("Failed to process offer.created event", e);
        }
    }
}
