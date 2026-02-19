package com.example.offerservice.service;

import com.example.offerservice.entity.OutboxEvent;
import com.example.offerservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Writes domain events to the outbox table (transactional with caller). Puts correlation ID from MDC into headers.
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
     * Persists event to outbox for later publishing. Caller must be in a transaction.
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
