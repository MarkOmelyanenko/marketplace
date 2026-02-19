package com.example.offerservice.consumer;

import com.example.offerservice.event.OfferEnrichedEvent;
import com.example.offerservice.service.OfferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OfferEnrichedConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(OfferEnrichedConsumer.class);
    
    private final OfferService offerService;
    private final ObjectMapper objectMapper;
    
    public OfferEnrichedConsumer(OfferService offerService, ObjectMapper objectMapper) {
        this.offerService = offerService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "offer.enriched", groupId = "offer-service")
    public void consume(String payload) {
        logger.info("Received raw offer.enriched message: {}", payload);
        try {
            OfferEnrichedEvent event = objectMapper.readValue(payload, OfferEnrichedEvent.class);
            logger.info("Parsed offer.enriched event: eventId={}, offerId={}, partnerId={}", 
                       event.getEventId(), event.getOfferId(), event.getPartnerId());
            
            offerService.handleOfferEnriched(
                event.getOfferId(),
                event.getPartnerId(),
                event.getAiTitle(),
                event.getAiDescription(),
                event.getAiTags()
            );
        } catch (Exception e) {
            logger.error("Error processing offer.enriched event: payload={}", payload, e);
            throw new RuntimeException("Failed to process offer.enriched event", e);
        }
    }
}
