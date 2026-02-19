package com.example.paymentsservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentResponse {
    
    private UUID id;
    private String partnerId;
    private UUID offerId;
    private Integer amountCents;
    private String currency;
    private String status;
    private String referenceType;
    private String providerPaymentId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getPartnerId() {
        return partnerId;
    }
    
    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }
    
    public UUID getOfferId() {
        return offerId;
    }
    
    public void setOfferId(UUID offerId) {
        this.offerId = offerId;
    }
    
    public Integer getAmountCents() {
        return amountCents;
    }
    
    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
    
    public String getProviderPaymentId() {
        return providerPaymentId;
    }
    
    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
