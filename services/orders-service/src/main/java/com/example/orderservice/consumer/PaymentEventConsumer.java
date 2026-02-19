package com.example.orderservice.consumer;

import com.example.orderservice.event.PaymentCapturedEvent;
import com.example.orderservice.event.PaymentFailedEvent;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    
    public PaymentEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "payment.captured", groupId = "orders-service")
    public void handlePaymentCaptured(String payload) {
        logger.info("Received raw payment.captured message: {}", payload);
        try {
            PaymentCapturedEvent event = objectMapper.readValue(payload, PaymentCapturedEvent.class);
            logger.info("Parsed payment.captured event: paymentId={}, referenceType={}, referenceId={}", 
                       event.getPaymentId(), event.getReferenceType(), event.getReferenceId());
            orderService.handlePaymentCaptured(event);
        } catch (Exception e) {
            logger.error("Error processing payment.captured event: payload={}", payload, e);
            throw new RuntimeException("Failed to process payment.captured event", e);
        }
    }
    
    @KafkaListener(topics = "payment.failed", groupId = "orders-service")
    public void handlePaymentFailed(String payload) {
        logger.info("Received raw payment.failed message: {}", payload);
        try {
            PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
            logger.info("Parsed payment.failed event: paymentId={}, referenceType={}, referenceId={}", 
                       event.getPaymentId(), event.getReferenceType(), event.getReferenceId());
            orderService.handlePaymentFailed(event);
        } catch (Exception e) {
            logger.error("Error processing payment.failed event: payload={}", payload, e);
            throw new RuntimeException("Failed to process payment.failed event", e);
        }
    }
}
