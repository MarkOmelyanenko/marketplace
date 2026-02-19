package com.example.paymentsservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentFailedEvent {
    
    @JsonProperty("eventId")
    private UUID eventId;
    
    @JsonProperty("occurredAt")
    private OffsetDateTime occurredAt;
    
    @JsonProperty("paymentId")
    private UUID paymentId;
    
    @JsonProperty("partnerId")
    private String partnerId;
    
    @JsonProperty("offerId")
    private UUID offerId;
    
    @JsonProperty("amountCents")
    private Integer amountCents;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("providerPaymentId")
    private String providerPaymentId;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("referenceType")
    private String referenceType;
    
    @JsonProperty("referenceId")
    private UUID referenceId;
    
    @JsonProperty("ownerType")
    private String ownerType;
    
    @JsonProperty("ownerId")
    private String ownerId;
    
    public PaymentFailedEvent() {
    }
    
    public PaymentFailedEvent(UUID eventId, OffsetDateTime occurredAt, UUID paymentId, 
                             String partnerId, UUID offerId, Integer amountCents, 
                             String currency, String providerPaymentId, String reason,
                             String referenceType, UUID referenceId, String ownerType, String ownerId) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.paymentId = paymentId;
        this.partnerId = partnerId;
        this.offerId = offerId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.providerPaymentId = providerPaymentId;
        this.reason = reason;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }
    
    public UUID getEventId() {
        return eventId;
    }
    
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
    
    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
    
    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
    
    public UUID getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
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
    
    public String getProviderPaymentId() {
        return providerPaymentId;
    }
    
    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
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
    
    public String getOwnerType() {
        return ownerType;
    }
    
    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
