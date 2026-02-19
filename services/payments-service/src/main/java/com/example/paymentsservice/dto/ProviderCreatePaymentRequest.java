package com.example.paymentsservice.dto;

import java.util.UUID;

public class ProviderCreatePaymentRequest {
    
    private UUID paymentId;
    private Integer amountCents;
    private String currency;
    
    public ProviderCreatePaymentRequest() {
    }
    
    public ProviderCreatePaymentRequest(UUID paymentId, Integer amountCents, String currency) {
        this.paymentId = paymentId;
        this.amountCents = amountCents;
        this.currency = currency;
    }
    
    public UUID getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
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
}
