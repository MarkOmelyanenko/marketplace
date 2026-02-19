package com.example.paymentsservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class ProviderWebhookRequest {
    
    @NotBlank(message = "providerPaymentId is required")
    @JsonProperty("providerPaymentId")
    private String providerPaymentId;
    
    @NotBlank(message = "status is required")
    @JsonProperty("status")
    private String status;
    
    @NotNull(message = "amountCents is required")
    @Min(value = 1, message = "amountCents must be positive")
    @JsonProperty("amountCents")
    private Integer amountCents;
    
    @NotBlank(message = "currency is required")
    @JsonProperty("currency")
    private String currency;
    
    @NotNull(message = "occurredAt is required")
    @JsonProperty("occurredAt")
    private OffsetDateTime occurredAt;
    
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
