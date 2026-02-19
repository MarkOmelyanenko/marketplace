package com.example.paymentsservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {
    
    private final Counter paymentsCreatedCounter;
    private final Counter webhooksReceivedCounter;
    private final Counter paymentsCapturedCounter;
    private final Counter paymentsFailedCounter;
    
    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.paymentsCreatedCounter = Counter.builder("payments_created_total")
            .description("Total number of payments created")
            .register(meterRegistry);
        
        this.webhooksReceivedCounter = Counter.builder("webhooks_received_total")
            .description("Total number of webhooks received")
            .register(meterRegistry);
        
        this.paymentsCapturedCounter = Counter.builder("payments_captured_total")
            .description("Total number of payments captured")
            .register(meterRegistry);
        
        this.paymentsFailedCounter = Counter.builder("payments_failed_total")
            .description("Total number of payments failed")
            .register(meterRegistry);
    }
    
    public void incrementPaymentsCreated() {
        paymentsCreatedCounter.increment();
    }
    
    public void incrementWebhooksReceived() {
        webhooksReceivedCounter.increment();
    }
    
    public void incrementPaymentsCaptured() {
        paymentsCapturedCounter.increment();
    }
    
    public void incrementPaymentsFailed() {
        paymentsFailedCounter.increment();
    }
}
