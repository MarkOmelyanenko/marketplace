package com.example.orderservice.controller;

import com.example.orderservice.dto.RefundRequestResponse;
import com.example.orderservice.dto.RejectRefundRequestDto;
import com.example.orderservice.service.RefundRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/refund-requests")
public class InternalRefundRequestController {

    private final RefundRequestService refundRequestService;

    public InternalRefundRequestController(RefundRequestService refundRequestService) {
        this.refundRequestService = refundRequestService;
    }

    @GetMapping
    @Operation(summary = "List refund requests", description = "List all refund requests (ops). Optional filter: status=PENDING")
    public ResponseEntity<List<RefundRequestResponse>> list(
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {

        String reviewedBy = getOpsToken(httpRequest);
        List<RefundRequestResponse> list = refundRequestService.listForOps(status);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve refund request")
    public ResponseEntity<RefundRequestResponse> approve(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        String reviewedBy = getOpsToken(httpRequest);
        RefundRequestResponse updated = refundRequestService.approve(id, reviewedBy);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject refund request", description = "Optionally provide rejection reason for the buyer")
    public ResponseEntity<RefundRequestResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RejectRefundRequestDto body,
            HttpServletRequest httpRequest) {

        String reviewedBy = getOpsToken(httpRequest);
        RefundRequestResponse updated = refundRequestService.reject(id, reviewedBy, body);
        return ResponseEntity.ok(updated);
    }

    private static String getOpsToken(HttpServletRequest request) {
        String token = request.getHeader("Ops-Token");
        return token != null && !token.isBlank() ? token : "ops";
    }
}
