package com.example.paymentsservice.service;

import com.example.paymentsservice.entity.OutboxEvent;
import com.example.paymentsservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Writes domain events to the outbox table (transactional with caller). Correlation ID from MDC
 * is stored in event headers when present.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists an event to the outbox for later publishing. Caller must be in a transaction.
     *
     * @param aggregateType e.g. PAYMENT, OFFER
     * @param aggregateId   entity id
     * @param topic         Kafka topic
     * @param event         payload (serialized to JSON)
     * @throws RuntimeException if serialization or save fails
     */
    @Transactional
    public void publishEvent(String aggregateType, UUID aggregateId, String topic, Object event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType(aggregateType);
            outboxEvent.setAggregateId(aggregateId);
            outboxEvent.setTopic(topic);
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            Map<String, String> headers = new HashMap<>();
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                headers.put("X-Correlation-Id", correlationId);
            }
            if (!headers.isEmpty()) {
                outboxEvent.setHeaders(objectMapper.writeValueAsString(headers));
            }
            
            outboxEvent.setStatus("NEW");
            outboxEvent.setAttempts(0);
            
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to outbox", e);
        }
    }
}
