package com.example.orderservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RefundRequestResponse {

    private UUID id;
    private UUID orderId;
    private String buyerId;
    private String reason;
    private String details;
    private String status;
    private Integer orderAmountCents;
    private OffsetDateTime createdAt;
    private OffsetDateTime reviewedAt;
    private String reviewedBy;
    private String rejectionReason;

    public RefundRequestResponse() {
    }

    public RefundRequestResponse(UUID id, UUID orderId, String buyerId, String reason, String details,
                                 String status, Integer orderAmountCents, OffsetDateTime createdAt, OffsetDateTime reviewedAt,
                                 String reviewedBy, String rejectionReason) {
        this.id = id;
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.reason = reason;
        this.details = details;
        this.status = status;
        this.orderAmountCents = orderAmountCents;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.rejectionReason = rejectionReason;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOrderAmountCents() {
        return orderAmountCents;
    }

    public void setOrderAmountCents(Integer orderAmountCents) {
        this.orderAmountCents = orderAmountCents;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
