package com.example.offerservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    @Bean
    public NewTopic offerCreatedTopic() {
        return TopicBuilder.name("offer.created")
                .partitions(1)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic offerEnrichedTopic() {
        return TopicBuilder.name("offer.enriched")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
