package com.example.paymentsservice.controller;

import com.example.paymentsservice.dto.PaymentResponse;
import com.example.paymentsservice.dto.RefundRequestDto;
import com.example.paymentsservice.dto.TimelineEventResponse;
import com.example.paymentsservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/payments")
public class InternalOpsController {
    
    private final PaymentService paymentService;
    
    public InternalOpsController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search or list payments", description = "Search by paymentId, offerId, partnerId, creation date (from/to), and status (PENDING, CAPTURED, FAILED).")
    public ResponseEntity<List<PaymentResponse>> searchPayments(
            @RequestParam(required = false) UUID paymentId,
            @RequestParam(required = false) UUID offerId,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(required = false) String status) {

        OffsetDateTime from = parseDateTime(createdFrom);
        OffsetDateTime to = parseDateTime(createdTo);
        List<PaymentResponse> results = paymentService.searchPayments(paymentId, offerId, partnerId, from, to, status);
        return ResponseEntity.ok(results);
    }

    private static OffsetDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            OffsetDateTime parsed = OffsetDateTime.parse(value);
            return parsed;
        } catch (DateTimeParseException e) {
            try {
                return java.time.LocalDate.parse(value).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
    
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details", description = "Get payment details by ID (ops access)")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        PaymentResponse payment = paymentService.getPaymentByIdForOps(paymentId);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/{paymentId}/timeline")
    @Operation(summary = "Get payment timeline", description = "Get timeline of events for a payment (ops access)")
    public ResponseEntity<List<TimelineEventResponse>> getTimeline(@PathVariable UUID paymentId) {
        List<TimelineEventResponse> timeline = paymentService.getPaymentTimelineForOps(paymentId);
        return ResponseEntity.ok(timeline);
    }
    
    @PostMapping("/{paymentId}/retry-latest-webhook")
    @Operation(summary = "Retry latest webhook", description = "Re-process the most recent provider webhook for a payment")
    public ResponseEntity<PaymentResponse> retryLatestWebhook(@PathVariable UUID paymentId) {
        PaymentResponse payment = paymentService.retryLatestWebhook(paymentId);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund payment", description = "Credit buyer wallet and mark payment as REFUNDED. Only CAPTURED ORDER_PURCHASE payments.")
    public ResponseEntity<PaymentResponse> refund(@Valid @RequestBody RefundRequestDto body) {
        PaymentResponse payment = paymentService.refundByPaymentId(body.getPaymentId());
        return ResponseEntity.ok(payment);
    }
}
