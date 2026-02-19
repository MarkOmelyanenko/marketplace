package com.example.offerservice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class CatalogOfferResponse {

    private UUID id;
    private String partnerId;
    private String title;
    private String description;
    private Integer priceCents;
    private String currency;
    private List<String> aiTags;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public CatalogOfferResponse() {
    }

    public CatalogOfferResponse(UUID id, String partnerId, String title, String description, Integer priceCents, String currency,
                                List<String> aiTags, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.title = title;
        this.description = description;
        this.priceCents = priceCents;
        this.currency = currency != null ? currency : "USD";
        this.aiTags = aiTags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
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

    public Integer getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(Integer priceCents) {
        this.priceCents = priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<String> getAiTags() {
        return aiTags;
    }
    
    public void setAiTags(List<String> aiTags) {
        this.aiTags = aiTags;
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
