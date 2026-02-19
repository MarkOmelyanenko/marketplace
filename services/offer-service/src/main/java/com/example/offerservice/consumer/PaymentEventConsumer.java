package com.example.offerservice.consumer;

import com.example.offerservice.event.PaymentCapturedEvent;
import com.example.offerservice.event.PaymentFailedEvent;
import com.example.offerservice.service.OfferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    private final OfferService offerService;
    private final ObjectMapper objectMapper;
    
    public PaymentEventConsumer(OfferService offerService, ObjectMapper objectMapper) {
        this.offerService = offerService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "payment.captured", groupId = "offer-service")
    public void consumePaymentCaptured(String payload) {
        logger.info("Received raw payment.captured message: {}", payload);
        try {
            PaymentCapturedEvent event = objectMapper.readValue(payload, PaymentCapturedEvent.class);
            logger.info("Parsed payment.captured event: eventId={}, offerId={}, partnerId={}", 
                       event.getEventId(), event.getOfferId(), event.getPartnerId());
            
            offerService.handlePaymentCaptured(event.getOfferId(), event.getPartnerId());
        } catch (Exception e) {
            logger.error("Error processing payment.captured event: payload={}", payload, e);
            throw new RuntimeException("Failed to process payment.captured event", e);
        }
    }
    
    @KafkaListener(topics = "payment.failed", groupId = "offer-service")
    public void consumePaymentFailed(String payload) {
        logger.info("Received raw payment.failed message: {}", payload);
        try {
            PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
            logger.info("Parsed payment.failed event: eventId={}, offerId={}, partnerId={}", 
                       event.getEventId(), event.getOfferId(), event.getPartnerId());
            
            offerService.handlePaymentFailed(event.getOfferId(), event.getPartnerId());
        } catch (Exception e) {
            logger.error("Error processing payment.failed event: payload={}", payload, e);
            throw new RuntimeException("Failed to process payment.failed event", e);
        }
    }
}
