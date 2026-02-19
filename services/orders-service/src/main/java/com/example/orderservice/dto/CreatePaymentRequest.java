package com.example.orderservice.dto;

import java.util.UUID;

public class CreatePaymentRequest {
    
    private UUID offerId;
    /** Partner ID of the offer (for ORDER_PURCHASE so partner sees payment in listMyPayments). */
    private String partnerId;
    private String referenceType;
    private UUID referenceId;
    private Integer amountCents;
    private String currency;
    
    public UUID getOfferId() {
        return offerId;
    }
    
    public void setOfferId(UUID offerId) {
        this.offerId = offerId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }
    
    public String getReferenceType() {
        return referenceType;
    }
    
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
    
    public UUID getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
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
