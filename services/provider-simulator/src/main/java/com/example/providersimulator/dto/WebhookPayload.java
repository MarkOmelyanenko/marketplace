package com.example.providersimulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public class WebhookPayload {
    
    @JsonProperty("providerPaymentId")
    private String providerPaymentId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("amountCents")
    private Integer amountCents;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("occurredAt")
    private OffsetDateTime occurredAt;
    
    public WebhookPayload() {
    }
    
    public WebhookPayload(String providerPaymentId, String status, Integer amountCents, 
                         String currency, OffsetDateTime occurredAt) {
        this.providerPaymentId = providerPaymentId;
        this.status = status;
        this.amountCents = amountCents;
        this.currency = currency;
        this.occurredAt = occurredAt;
    }
    
    public String getProviderPaymentId() {
        return providerPaymentId;
    }
    
    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
    
    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
