package com.example.paymentsservice.service;

import com.example.paymentsservice.entity.OutboxEvent;
import com.example.paymentsservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Polls the outbox table and publishes pending events to Kafka. Scheduled every second.
 * Uses pessimistic locking; if markAsSending returns 0, another instance claimed the batch.
 */
@Service
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes up to {@value #BATCH_SIZE} pending outbox events to Kafka. Transactional.
     * Skips batch if another instance already marked them as SENDING.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> events = outboxEventRepository.findPendingEventsForPublishing();
            if (events.isEmpty()) {
                return;
            }
            List<OutboxEvent> batch = events.stream()
                .limit(BATCH_SIZE)
                .toList();
            List<UUID> ids = batch.stream().map(OutboxEvent::getId).toList();
            int updated = outboxEventRepository.markAsSending(ids);
            if (updated == 0) {
                return;
            }
            List<OutboxEvent> eventsToPublish = outboxEventRepository.findAllById(ids);
            
            for (OutboxEvent event : eventsToPublish) {
                try {
                    publishEvent(event);
                    event.setStatus("SENT");
                    event.setLastError(null);
                } catch (Exception e) {
                    logger.error("Failed to publish outbox event: eventId={}, topic={}", 
                               event.getId(), event.getTopic(), e);
                    event.setStatus("FAILED");
                    event.setAttempts(event.getAttempts() + 1);
                    event.setLastError(e.getMessage());
                }
                outboxEventRepository.save(event);
            }
            
        } catch (Exception e) {
            logger.error("Error in outbox publisher", e);
        }
    }

    private void publishEvent(OutboxEvent event) throws Exception {
        Map<String, String> headers = new HashMap<>();
        if (event.getHeaders() != null && !event.getHeaders().isEmpty()) {
            try {
                Map<String, String> parsedHeaders = objectMapper.readValue(
                    event.getHeaders(), 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)
                );
                headers.putAll(parsedHeaders);
            } catch (Exception e) {
                logger.warn("Failed to parse headers for event: {}", event.getId(), e);
            }
        }
        String correlationId = headers.getOrDefault("X-Correlation-Id", MDC.get("correlationId"));
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        headers.put("X-Correlation-Id", correlationId);
        Object payload = objectMapper.readValue(event.getPayload(), Object.class);
        MessageBuilder<Object> messageBuilder = MessageBuilder.withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, event.getTopic())
            .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString());
        for (Map.Entry<String, String> header : headers.entrySet()) {
            messageBuilder.setHeader(header.getKey(), header.getValue());
        }
        kafkaTemplate.send(messageBuilder.build()).get();
        
        logger.info("Published outbox event: eventId={}, topic={}, aggregateId={}", 
                   event.getId(), event.getTopic(), event.getAggregateId());
    }
}
