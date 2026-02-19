package com.example.enrichmentservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class OfferEnrichedEvent {
    
    @JsonProperty("eventId")
    private UUID eventId;
    
    @JsonProperty("occurredAt")
    private OffsetDateTime occurredAt;
    
    @JsonProperty("offerId")
    private UUID offerId;
    
    @JsonProperty("partnerId")
    private String partnerId;
    
    @JsonProperty("aiTitle")
    private String aiTitle;
    
    @JsonProperty("aiDescription")
    private String aiDescription;
    
    @JsonProperty("aiTags")
    private List<String> aiTags;
    
    public OfferEnrichedEvent() {
    }
    
    public OfferEnrichedEvent(UUID eventId, OffsetDateTime occurredAt, UUID offerId, String partnerId, 
                             String aiTitle, String aiDescription, List<String> aiTags) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.offerId = offerId;
        this.partnerId = partnerId;
        this.aiTitle = aiTitle;
        this.aiDescription = aiDescription;
        this.aiTags = aiTags;
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
    
    public String getAiTitle() {
        return aiTitle;
    }
    
    public void setAiTitle(String aiTitle) {
        this.aiTitle = aiTitle;
    }
    
    public String getAiDescription() {
        return aiDescription;
    }
    
    public void setAiDescription(String aiDescription) {
        this.aiDescription = aiDescription;
    }
    
    public List<String> getAiTags() {
        return aiTags;
    }
    
    public void setAiTags(List<String> aiTags) {
        this.aiTags = aiTags;
    }
}
