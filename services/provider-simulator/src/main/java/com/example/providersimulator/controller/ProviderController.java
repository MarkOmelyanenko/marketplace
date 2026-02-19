package com.example.providersimulator.controller;

import com.example.providersimulator.dto.CreatePaymentRequest;
import com.example.providersimulator.dto.CreatePaymentResponse;
import com.example.providersimulator.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/provider")
public class ProviderController {
    
    private final ProviderService providerService;
    
    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }
    
    @PostMapping("/payments")
    @Operation(summary = "Create a payment at provider", description = "Simulates creating a payment at a payment service provider")
    public ResponseEntity<CreatePaymentResponse> createPayment(@RequestBody CreatePaymentRequest request) {
        CreatePaymentResponse response = providerService.createPayment(request);
        return ResponseEntity.ok(response);
    }
}
