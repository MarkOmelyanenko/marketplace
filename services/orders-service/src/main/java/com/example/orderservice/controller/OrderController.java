package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateRefundRequestDto;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.RefundRequestResponse;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.RefundRequestService;
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
 * REST API for orders: create, list by buyer, get by id, retry payment, cancel, create refund request.
 * Expects Buyer-Id from interceptor (set on request attributes).
 */
@RestController
@RequestMapping("/v1/orders")
public class OrderController {
    
    private final OrderService orderService;
    private final RefundRequestService refundRequestService;

    public OrderController(OrderService orderService, RefundRequestService refundRequestService) {
        this.orderService = orderService;
        this.refundRequestService = refundRequestService;
    }
    
    @PostMapping
    @Operation(
        summary = "Create a new order",
        description = "Creates a new order for a published offer and initiates payment",
        security = @SecurityRequirement(name = "Buyer-Id")
    )
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        String buyerId = (String) httpRequest.getAttribute("buyerId");
        OrderResponse response = orderService.createOrder(request, buyerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(
        summary = "List orders",
        description = "Retrieves all orders for the authenticated buyer",
        security = @SecurityRequirement(name = "Buyer-Id")
    )
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(value = "mine", required = false, defaultValue = "true") boolean mine,
            HttpServletRequest httpRequest) {
        
        String buyerId = (String) httpRequest.getAttribute("buyerId");
        List<OrderResponse> orders = orderService.getOrdersByBuyer(buyerId);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get order by ID",
        description = "Retrieves a specific order by ID (scoped to buyer)",
        security = @SecurityRequirement(name = "Buyer-Id")
    )
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        
        String buyerId = (String) httpRequest.getAttribute("buyerId");
        OrderResponse order = orderService.getOrderById(id, buyerId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/retry-payment")
    @Operation(
        summary = "Retry payment for order",
        description = "Retries payment for an order that is awaiting payment (e.g. after topping up balance)",
        security = @SecurityRequirement(name = "Buyer-Id")
    )
    public ResponseEntity<OrderResponse> retryPayment(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        
        String buyerId = (String) httpRequest.getAttribute("buyerId");
        OrderResponse order = orderService.retryPayment(id, buyerId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/cancel")
    @Operation(
        summary = "Cancel order",
        description = "Cancels an order that is awaiting payment (PENDING_PAYMENT). Only the order owner can cancel.",
        security = @SecurityRequirement(name = "Buyer-Id")
    )
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        
        String buyerId = (String) httpRequest.getAttribute("buyerId");
        OrderResponse order = orderService.cancelOrder(id, buyerId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/refund-request")
    @Operation(
        summary = "Request refund for paid order",
        description = "Creates a refund request for a PAID order (reason and optional details). One pending request per order.",
        security = @SecurityRequirement(name = "Buyer-Id")
    )
    public ResponseEntity<RefundRequestResponse> createRefundRequest(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRefundRequestDto request,
            HttpServletRequest httpRequest) {

        String buyerId = (String) httpRequest.getAttribute("buyerId");
        RefundRequestResponse created = refundRequestService.create(id, buyerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
