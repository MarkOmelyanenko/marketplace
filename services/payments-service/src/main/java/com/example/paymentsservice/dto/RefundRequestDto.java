package com.example.paymentsservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class RefundRequestDto {

    @NotNull(message = "paymentId is required")
    private UUID paymentId;

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }
}
