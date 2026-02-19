package com.example.providersimulator.dto;

public class CreatePaymentResponse {
    
    private String providerPaymentId;
    
    public CreatePaymentResponse() {
    }
    
    public CreatePaymentResponse(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }
    
    public String getProviderPaymentId() {
        return providerPaymentId;
    }
    
    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }
}
