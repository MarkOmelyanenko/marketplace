package com.example.paymentsservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DepositRequest {

    @NotNull(message = "amountCents is required")
    @Min(value = 1, message = "amountCents must be positive")
    private Long amountCents;

    @Size(min = 3, max = 3, message = "currency must be 3 characters")
    private String currency = "USD";

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
