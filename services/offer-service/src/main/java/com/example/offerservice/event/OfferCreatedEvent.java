package com.example.offerservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class OfferCreatedEvent {
    
    @JsonProperty("eventId")
    private UUID eventId;
    
    @JsonProperty("occurredAt")
    private OffsetDateTime occurredAt;
    
    @JsonProperty("offerId")
    private UUID offerId;
    
    @JsonProperty("partnerId")
    private String partnerId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("description")
    private String description;
    
    public OfferCreatedEvent() {
    }
    
    public OfferCreatedEvent(UUID eventId, OffsetDateTime occurredAt, UUID offerId, String partnerId, String title, String description) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.offerId = offerId;
        this.partnerId = partnerId;
        this.title = title;
        this.description = description;
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
