package com.example.orderservice.service;

import com.example.orderservice.client.PaymentServiceClient;
import com.example.orderservice.dto.CreateRefundRequestDto;
import com.example.orderservice.dto.RefundRequestResponse;
import com.example.orderservice.dto.RejectRefundRequestDto;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.RefundRequest;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.RefundRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Refund requests: create (buyer), list by order/buyer, ops list/approve/reject.
 * Approve triggers payment refund via payments-service. Transactional where applicable.
 */
@Service
public class RefundRequestService {

    private static final Logger logger = LoggerFactory.getLogger(RefundRequestService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;

    public RefundRequestService(RefundRequestRepository refundRequestRepository,
                                OrderRepository orderRepository,
                                PaymentServiceClient paymentServiceClient) {
        this.refundRequestRepository = refundRequestRepository;
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
    }

    @Transactional
    public RefundRequestResponse create(UUID orderId, String buyerId, CreateRefundRequestDto dto) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!"PAID".equals(order.getStatus())) {
            throw new RuntimeException("Refund can be requested only for paid orders");
        }
        if (refundRequestRepository.findByOrderIdAndStatus(orderId, STATUS_PENDING).isPresent()) {
            throw new RuntimeException("A refund request for this order is already pending");
        }

        RefundRequest req = new RefundRequest();
        req.setOrderId(orderId);
        req.setBuyerId(buyerId);
        req.setReason(dto.getReason().trim());
        req.setDetails(dto.getDetails() != null ? dto.getDetails().trim() : null);
        req.setStatus(STATUS_PENDING);
        req.setCreatedAt(OffsetDateTime.now());
        req = refundRequestRepository.save(req);
        logger.info("Refund request created: id={}, orderId={}, buyerId={}", req.getId(), orderId, buyerId);
        return toResponse(req);
    }

    public RefundRequestResponse getByOrderAndBuyer(UUID orderId, String buyerId) {
        return refundRequestRepository.findByOrderIdAndBuyerIdOrderByCreatedAtDesc(orderId, buyerId)
            .stream()
            .findFirst()
            .map(this::toResponse)
            .orElse(null);
    }

    public List<RefundRequest> findByOrderIdOrderByCreatedAtDesc(UUID orderId) {
        return refundRequestRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    public List<RefundRequestResponse> listForOps(String statusFilter) {
        List<RefundRequest> list = statusFilter != null && !statusFilter.isBlank()
            ? refundRequestRepository.findByStatusOrderByCreatedAtDesc(statusFilter)
            : refundRequestRepository.findAllByOrderByCreatedAtDesc();
        return list.stream()
            .map(req -> {
                Integer orderAmountCents = orderRepository.findById(req.getOrderId())
                    .map(Order::getAmountCents)
                    .orElse(null);
                return toResponse(req, orderAmountCents);
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public RefundRequestResponse approve(UUID refundRequestId, String reviewedBy) {
        RefundRequest req = refundRequestRepository.findById(refundRequestId)
            .orElseThrow(() -> new RuntimeException("Refund request not found"));
        if (!STATUS_PENDING.equals(req.getStatus())) {
            throw new RuntimeException("Refund request is not pending");
        }

        Order order = orderRepository.findById(req.getOrderId())
            .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getPaymentId() == null) {
            throw new RuntimeException("Order has no payment to refund");
        }

        paymentServiceClient.refund(order.getPaymentId()).block();
        logger.info("Payment refunded: paymentId={}", order.getPaymentId());

        req.setStatus(STATUS_APPROVED);
        req.setReviewedAt(OffsetDateTime.now());
        req.setReviewedBy(reviewedBy);
        refundRequestRepository.save(req);
        logger.info("Refund request approved: id={}, orderId={}, reviewedBy={}", req.getId(), req.getOrderId(), reviewedBy);
        return toResponse(req);
    }

    @Transactional
    public RefundRequestResponse reject(UUID refundRequestId, String reviewedBy, RejectRefundRequestDto dto) {
        RefundRequest req = refundRequestRepository.findById(refundRequestId)
            .orElseThrow(() -> new RuntimeException("Refund request not found"));
        if (!STATUS_PENDING.equals(req.getStatus())) {
            throw new RuntimeException("Refund request is not pending");
        }

        req.setStatus(STATUS_REJECTED);
        req.setReviewedAt(OffsetDateTime.now());
        req.setReviewedBy(reviewedBy);
        req.setRejectionReason(dto != null && dto.getRejectionReason() != null ? dto.getRejectionReason().trim() : null);
        refundRequestRepository.save(req);
        logger.info("Refund request rejected: id={}, orderId={}, reviewedBy={}", req.getId(), req.getOrderId(), reviewedBy);
        return toResponse(req);
    }

    public RefundRequestResponse toResponse(RefundRequest req) {
        return toResponse(req, null);
    }

    public RefundRequestResponse toResponse(RefundRequest req, Integer orderAmountCents) {
        return new RefundRequestResponse(
            req.getId(),
            req.getOrderId(),
            req.getBuyerId(),
            req.getReason(),
            req.getDetails(),
            req.getStatus(),
            orderAmountCents,
            req.getCreatedAt(),
            req.getReviewedAt(),
            req.getReviewedBy(),
            req.getRejectionReason()
        );
    }
}
