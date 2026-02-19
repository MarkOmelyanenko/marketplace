package com.example.paymentsservice.controller;

import com.example.paymentsservice.dto.CreatePaymentRequest;
import com.example.paymentsservice.dto.PaymentResponse;
import com.example.paymentsservice.dto.TimelineEventResponse;
import com.example.paymentsservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for payments: create (with idempotency), list by partner, get by id, and timeline.
 * Expects Partner-Id or Buyer-Id from interceptors (set on request attributes).
 */
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @PostMapping
    @Operation(
        summary = "Create a new payment",
        description = "Creates a new payment request. Supports partner listing fees and buyer order purchases. Supports idempotency via Idempotency-Key header.",
        security = @SecurityRequirement(name = "Partner-Id or Buyer-Id")
    )
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        String ownerType = (String) httpRequest.getAttribute("ownerType");
        String ownerId = (String) httpRequest.getAttribute("ownerId");

        if (request.getReferenceType() == null || request.getReferenceType().isBlank()) {
            if (partnerId != null) {
                request.setReferenceType("OFFER_LISTING");
            } else {
                request.setReferenceType("ORDER_PURCHASE");
            }
        }
        if ((partnerId == null || partnerId.isBlank()) && "ORDER_PURCHASE".equals(request.getReferenceType())
                && request.getPartnerId() != null && !request.getPartnerId().isBlank()) {
            partnerId = request.getPartnerId();
        }
        String idempotencyLookupId = partnerId != null ? partnerId : ownerId;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingKey = paymentService.findIdempotencyKey(idempotencyKey, idempotencyLookupId);
            if (existingKey.isPresent()) {
                PaymentResponse existingPayment = paymentService.getPaymentById(existingKey.get().getPaymentId(), idempotencyLookupId);
                return ResponseEntity.status(HttpStatus.OK).body(existingPayment);
            }
        }
        
        PaymentResponse response = paymentService.createPayment(request, ownerType, ownerId, partnerId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
        summary = "List my payments",
        description = "Returns payments for the authenticated partner (for stats/dashboard).",
        security = @SecurityRequirement(name = "Partner-Id or Buyer-Id")
    )
    public ResponseEntity<List<PaymentResponse>> listMyPayments(HttpServletRequest httpRequest) {
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        if (partnerId == null || partnerId.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<PaymentResponse> payments = paymentService.getPaymentsByPartnerId(partnerId);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get payment by ID",
        description = "Retrieves a specific payment by ID (scoped to owner)",
        security = @SecurityRequirement(name = "Partner-Id or Buyer-Id")
    )
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        
        String ownerId = (String) httpRequest.getAttribute("ownerId");
        PaymentResponse payment = paymentService.getPaymentById(id, ownerId);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/{id}/timeline")
    @Operation(
        summary = "Get payment timeline",
        description = "Retrieves the timeline of events for a payment",
        security = @SecurityRequirement(name = "Partner-Id or Buyer-Id")
    )
    public ResponseEntity<List<TimelineEventResponse>> getPaymentTimeline(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        
        String ownerId = (String) httpRequest.getAttribute("ownerId");
        List<TimelineEventResponse> timeline = paymentService.getPaymentTimeline(id, ownerId);
        return ResponseEntity.ok(timeline);
    }
}
