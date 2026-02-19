package com.example.orderservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderResponse {
    
    private UUID id;
    private String buyerId;
    private UUID offerId;
    private String offerTitle;
    private Integer amountCents;
    private Integer quantity;
    private String currency;
    private String status;
    private UUID paymentId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private RefundRequestResponse refundRequest;

    public OrderResponse() {
    }

    public OrderResponse(UUID id, String buyerId, UUID offerId, String offerTitle,
                        Integer amountCents, Integer quantity, String currency, String status,
                        UUID paymentId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, buyerId, offerId, offerTitle, amountCents, quantity, currency, status, paymentId, createdAt, updatedAt, null);
    }

    public OrderResponse(UUID id, String buyerId, UUID offerId, String offerTitle,
                        Integer amountCents, Integer quantity, String currency, String status,
                        UUID paymentId, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                        RefundRequestResponse refundRequest) {
        this.id = id;
        this.buyerId = buyerId;
        this.offerId = offerId;
        this.offerTitle = offerTitle;
        this.amountCents = amountCents;
        this.quantity = quantity != null ? quantity : 1;
        this.currency = currency;
        this.status = status;
        this.paymentId = paymentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.refundRequest = refundRequest;
    }

    public RefundRequestResponse getRefundRequest() {
        return refundRequest;
    }

    public void setRefundRequest(RefundRequestResponse refundRequest) {
        this.refundRequest = refundRequest;
    }

    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getBuyerId() {
        return buyerId;
    }
    
    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }
    
    public UUID getOfferId() {
        return offerId;
    }
    
    public void setOfferId(UUID offerId) {
        this.offerId = offerId;
    }
    
    public String getOfferTitle() {
        return offerTitle;
    }
    
    public void setOfferTitle(String offerTitle) {
        this.offerTitle = offerTitle;
    }
    
    public Integer getAmountCents() {
        return amountCents;
    }
    
    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }

    public Integer getQuantity() {
        return quantity != null ? quantity : 1;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public UUID getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
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
