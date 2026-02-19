package com.example.offerservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class Offer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "partner_id", nullable = false)
    private String partnerId;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "title", nullable = false, length = 140)
    private String title;
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_cents")
    private Integer priceCents;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "ai_title", length = 140)
    private String aiTitle;
    
    @Column(name = "ai_description", columnDefinition = "TEXT")
    private String aiDescription;
    
    @Column(name = "ai_tags", columnDefinition = "TEXT")
    private String aiTags;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public String getAiTags() {
        return aiTags;
    }
    
    public void setAiTags(String aiTags) {
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
