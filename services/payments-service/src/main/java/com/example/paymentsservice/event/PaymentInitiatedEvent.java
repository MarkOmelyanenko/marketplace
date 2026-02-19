package com.example.paymentsservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentInitiatedEvent {
    
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
    
    public PaymentInitiatedEvent() {
    }
    
    public PaymentInitiatedEvent(UUID eventId, OffsetDateTime occurredAt, UUID paymentId, 
                                 String partnerId, UUID offerId, Integer amountCents, String currency) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.paymentId = paymentId;
        this.partnerId = partnerId;
        this.offerId = offerId;
        this.amountCents = amountCents;
        this.currency = currency;
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
}
