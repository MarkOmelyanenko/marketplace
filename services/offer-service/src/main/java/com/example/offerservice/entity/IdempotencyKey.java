package com.example.offerservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    
    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    
    @Column(name = "partner_id", nullable = false)
    private String partnerId;
    
    @Column(name = "offer_id", nullable = false)
    private java.util.UUID offerId;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public String getPartnerId() {
        return partnerId;
    }
    
    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }
    
    public java.util.UUID getOfferId() {
        return offerId;
    }
    
    public void setOfferId(java.util.UUID offerId) {
        this.offerId = offerId;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
