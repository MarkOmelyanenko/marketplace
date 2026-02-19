package com.example.offerservice.config;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Kafka consumer interceptor. Currently passes records through; MDC is set by outbox publisher when processing.
 */
public class KafkaCorrelationInterceptor implements ConsumerInterceptor<Object, Object> {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
    }
    
    @Override
    public void close() {
        MDC.clear();
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
