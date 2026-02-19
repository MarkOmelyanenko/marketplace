package com.example.orderservice.service;

import com.example.orderservice.client.OfferServiceClient;
import com.example.orderservice.client.PaymentServiceClient;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.exception.InsufficientBalanceException;
import com.example.orderservice.event.PaymentCapturedEvent;
import com.example.orderservice.event.PaymentFailedEvent;
import com.example.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order lifecycle: create (with payment), list by buyer, get by id, retry payment, cancel.
 * Consumes payment.captured / payment.failed from Kafka to update order status.
 */
@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final int DEFAULT_ORDER_PRICE_CENTS = 499;
    
    private final OrderRepository orderRepository;
    private final OfferServiceClient offerServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final RefundRequestService refundRequestService;

    public OrderService(OrderRepository orderRepository,
                       OfferServiceClient offerServiceClient,
                       PaymentServiceClient paymentServiceClient,
                       RefundRequestService refundRequestService) {
        this.orderRepository = orderRepository;
        this.offerServiceClient = offerServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.refundRequestService = refundRequestService;
    }
    
    /**
     * Creates an order, calls payments-service to charge buyer, and updates order to PAID on success.
     * On payment failure order remains PENDING_PAYMENT for retry. Transactional.
     *
     * @param request  offerId required; quantity optional (default 1)
     * @param buyerId  authenticated buyer
     * @return order response (status PAID or PENDING_PAYMENT)
     * @throws RuntimeException if offer not found or not published
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String buyerId) {
        CatalogOfferResponse offer = offerServiceClient.getPublishedOffer(request.getOfferId())
            .block();
        if (offer == null) {
            throw new RuntimeException("Offer not found or not published");
        }
        int qty = request.getQuantity() != null && request.getQuantity() >= 1 ? request.getQuantity() : 1;
        int unitPriceCents = (offer.getPriceCents() != null && offer.getPriceCents() > 0)
            ? offer.getPriceCents() : DEFAULT_ORDER_PRICE_CENTS;
        int amountCents = unitPriceCents * qty;
        String currency = (offer.getCurrency() != null && !offer.getCurrency().isBlank())
            ? offer.getCurrency() : "USD";

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setOfferId(request.getOfferId());
        order.setPartnerId(offer.getPartnerId());
        order.setOfferTitleSnapshot(offer.getTitle());
        order.setAmountCents(amountCents);
        order.setQuantity(qty);
        order.setCurrency(currency);
        order.setStatus("PENDING_PAYMENT");

        Order savedOrder = orderRepository.save(order);

        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOfferId(request.getOfferId());
        paymentRequest.setPartnerId(offer.getPartnerId());
        paymentRequest.setReferenceType("ORDER_PURCHASE");
        paymentRequest.setReferenceId(savedOrder.getId());
        paymentRequest.setAmountCents(amountCents);
        paymentRequest.setCurrency(currency);
        
        try {
            PaymentResponse paymentResponse = paymentServiceClient.createPayment(paymentRequest, buyerId)
                .block();
            if (paymentResponse != null && "CAPTURED".equals(paymentResponse.getStatus())) {
                savedOrder.setPaymentId(paymentResponse.getId());
                savedOrder.setStatus("PAID");
                savedOrder = orderRepository.save(savedOrder);
                logger.info("Created order with payment: orderId={}, paymentId={}", savedOrder.getId(), paymentResponse.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to create payment for order: orderId={}", savedOrder.getId(), e);
        }
        return toResponse(savedOrder);
    }

    /**
     * Lists orders for a buyer, newest first. Read-only.
     */
    public List<OrderResponse> getOrdersByBuyer(String buyerId) {
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        return orders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Returns an order by id if it belongs to the buyer. Includes refund request if present. Read-only.
     *
     * @param id      order id
     * @param buyerId must match order's buyerId
     * @return order response with optional refund request
     * @throws RuntimeException if order not found or buyer mismatch
     */
    public OrderResponse getOrderById(UUID id, String buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(id, buyerId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        OrderResponse response = toResponse(order);
        RefundRequestResponse refundRequest = refundRequestService.getByOrderAndBuyer(id, buyerId);
        if (refundRequest != null) {
            response.setRefundRequest(refundRequest);
        }
        return response;
    }

    /**
     * Retries payment for an order in PENDING_PAYMENT. Uses a unique idempotency key per call.
     * If payment succeeds, order is updated to PAID. Transactional.
     *
     * @param orderId order id
     * @param buyerId must match order's buyerId
     * @return updated order response
     * @throws RuntimeException if order not found, not PENDING_PAYMENT, or payment fails
     * @throws InsufficientBalanceException if payment returns FAILED (insufficient balance)
     */
    @Transactional
    public OrderResponse retryPayment(UUID orderId, String buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("Order is not awaiting payment");
        }
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOfferId(order.getOfferId());
        paymentRequest.setPartnerId(order.getPartnerId());
        paymentRequest.setReferenceType("ORDER_PURCHASE");
        paymentRequest.setReferenceId(order.getId());
        paymentRequest.setAmountCents(order.getAmountCents());
        paymentRequest.setCurrency(order.getCurrency());
        String idempotencyKey = "retry-" + orderId + "-" + System.currentTimeMillis();
        PaymentResponse paymentResponse = paymentServiceClient.createPayment(paymentRequest, buyerId, idempotencyKey)
            .block();
        if (paymentResponse == null) {
            throw new RuntimeException("Payment failed");
        }
        if ("FAILED".equals(paymentResponse.getStatus())) {
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }
        if ("CAPTURED".equals(paymentResponse.getStatus())) {
            order.setPaymentId(paymentResponse.getId());
            order.setStatus("PAID");
            orderRepository.save(order);
            logger.info("Order paid after retry: orderId={}", orderId);
        }
        return toResponse(orderRepository.findById(orderId).orElse(order));
    }

    /**
     * Cancels an order in PENDING_PAYMENT. Buyer-initiated. Transactional.
     *
     * @param orderId order id
     * @param buyerId must match order's buyerId
     * @return updated order response
     * @throws RuntimeException if order not found or not PENDING_PAYMENT
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("Only orders awaiting payment can be cancelled");
        }
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        logger.info("Order cancelled by buyer: orderId={}", orderId);
        return toResponse(order);
    }

    /**
     * Kafka consumer handler: marks order PAID when payment.captured is received for ORDER_PURCHASE.
     * Ignores events for other reference types or when buyerId does not match. Transactional.
     */
    @Transactional
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        if (!"ORDER_PURCHASE".equals(event.getReferenceType()) || event.getReferenceId() == null) {
            return;
        }
        UUID orderId = event.getReferenceId();
        Order order = orderRepository.findById(orderId)
            .orElse(null);
        if (order == null) {
            logger.warn("Order not found for payment captured: orderId={}", orderId);
            return;
        }
        if (!order.getBuyerId().equals(event.getOwnerId())) {
            logger.warn("BuyerId mismatch for order: orderId={}, orderBuyerId={}, eventOwnerId={}", 
                       orderId, order.getBuyerId(), event.getOwnerId());
            return;
        }
        if ("PENDING_PAYMENT".equals(order.getStatus())) {
            order.setStatus("PAID");
            orderRepository.save(order);
            logger.info("Order marked as PAID: orderId={}", orderId);
        }
    }

    /**
     * Kafka consumer handler: on payment.failed for ORDER_PURCHASE, leaves order in PENDING_PAYMENT
     * so buyer can retry (does not set to CANCELLED). Ignores non-matching reference or buyerId.
     */
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        if (!"ORDER_PURCHASE".equals(event.getReferenceType()) || event.getReferenceId() == null) {
            return;
        }
        UUID orderId = event.getReferenceId();
        Order order = orderRepository.findById(orderId)
            .orElse(null);
        if (order == null) {
            logger.warn("Order not found for payment failed: orderId={}", orderId);
            return;
        }
        if (!order.getBuyerId().equals(event.getOwnerId())) {
            logger.warn("BuyerId mismatch for order: orderId={}, orderBuyerId={}, eventOwnerId={}",
                       orderId, order.getBuyerId(), event.getOwnerId());
            return;
        }
        if ("PENDING_PAYMENT".equals(order.getStatus())) {
            logger.info("Payment failed for order (e.g. insufficient balance), leaving as PENDING_PAYMENT for retry: orderId={}", orderId);
        }
    }
    
    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getBuyerId(),
            order.getOfferId(),
            order.getOfferTitleSnapshot(),
            order.getAmountCents(),
            order.getQuantity(),
            order.getCurrency(),
            order.getStatus(),
            order.getPaymentId(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
