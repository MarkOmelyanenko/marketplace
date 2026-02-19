package com.example.offerservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OfferMetrics {
    
    private final Counter offersCreatedCounter;
    private final Counter offersEnrichedCounter;
    
    public OfferMetrics(MeterRegistry meterRegistry) {
        this.offersCreatedCounter = Counter.builder("offers_created_total")
            .description("Total number of offers created")
            .register(meterRegistry);
        
        this.offersEnrichedCounter = Counter.builder("offers_enriched_total")
            .description("Total number of offers enriched")
            .register(meterRegistry);
    }
    
    public void incrementOffersCreated() {
        offersCreatedCounter.increment();
    }
    
    public void incrementOffersEnriched() {
        offersEnrichedCounter.increment();
    }
}
