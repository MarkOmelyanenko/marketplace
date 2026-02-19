package com.example.paymentsservice.dto;

public class ProviderCreatePaymentResponse {
    
    private String providerPaymentId;
    
    public ProviderCreatePaymentResponse() {
    }
    
    public ProviderCreatePaymentResponse(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }
    
    public String getProviderPaymentId() {
        return providerPaymentId;
    }
    
    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }
}
