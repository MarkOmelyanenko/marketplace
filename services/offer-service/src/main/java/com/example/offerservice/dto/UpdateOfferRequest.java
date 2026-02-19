package com.example.offerservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateOfferRequest {

    @Size(max = 140, message = "Title must not exceed 140 characters")
    private String title;

    private String description;

    @Min(value = 1, message = "Price must be at least 0.01 USD")
    private Integer priceCents;

    @Size(max = 3)
    private String currency;

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
}
