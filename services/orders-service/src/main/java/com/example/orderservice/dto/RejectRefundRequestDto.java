package com.example.orderservice.dto;

import jakarta.validation.constraints.Size;

public class RejectRefundRequestDto {

    @Size(max = 1000)
    private String rejectionReason;

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
