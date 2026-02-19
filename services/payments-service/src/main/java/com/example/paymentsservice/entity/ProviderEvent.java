package com.example.paymentsservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "provider_events")
public class ProviderEvent {
    
    @Id
    @Column(name = "provider_event_id")
    private String providerEventId;
    
    @Column(name = "provider_payment_id", nullable = false)
    private String providerPaymentId;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;
    
    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = OffsetDateTime.now();
        }
    }
    
    public String getProviderEventId() {
        return providerEventId;
    }
    
    public void setProviderEventId(String providerEventId) {
        this.providerEventId = providerEventId;
    }
    
    public String getProviderPaymentId() {
        return providerPaymentId;
    }
    
    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }
    
    public UUID getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
