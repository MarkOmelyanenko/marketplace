package com.example.paymentsservice.service;

import com.example.paymentsservice.client.ProviderClient;
import com.example.paymentsservice.dto.*;
import com.example.paymentsservice.entity.IdempotencyKey;
import com.example.paymentsservice.entity.Payment;
import com.example.paymentsservice.entity.ProviderEvent;
import com.example.paymentsservice.service.WalletService;
import com.example.paymentsservice.event.PaymentCapturedEvent;
import com.example.paymentsservice.event.PaymentFailedEvent;
import com.example.paymentsservice.event.PaymentInitiatedEvent;
import com.example.paymentsservice.repository.IdempotencyKeyRepository;
import com.example.paymentsservice.repository.PaymentRepository;
import com.example.paymentsservice.repository.ProviderEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core payment lifecycle: create (with optional idempotency), wallet debit, status updates,
 * provider webhooks, and outbox-based Kafka events. All create/webhook/refund operations
 * are transactional and may perform DB writes and outbox publishes.
 */
@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_INITIATED_TOPIC = "payment.initiated";
    private static final String PAYMENT_CAPTURED_TOPIC = "payment.captured";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ProviderEventRepository providerEventRepository;
    private final ProviderClient providerClient;
    private final WalletService walletService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxService outboxService;
    private final com.example.paymentsservice.metrics.PaymentMetrics paymentMetrics;

    public PaymentService(PaymentRepository paymentRepository,
                         IdempotencyKeyRepository idempotencyKeyRepository,
                         ProviderEventRepository providerEventRepository,
                         ProviderClient providerClient,
                         WalletService walletService,
                         KafkaTemplate<String, Object> kafkaTemplate,
                         OutboxService outboxService,
                         com.example.paymentsservice.metrics.PaymentMetrics paymentMetrics) {
        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.providerEventRepository = providerEventRepository;
        this.providerClient = providerClient;
        this.walletService = walletService;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxService = outboxService;
        this.paymentMetrics = paymentMetrics;
    }
    
    /**
     * Looks up a stored idempotency key for the given key and owner (partnerId or ownerId).
     *
     * @param idempotencyKey client-provided idempotency key
     * @param partnerId      partner or owner id used when storing the key
     * @return stored key if present
     */
    public Optional<IdempotencyKey> findIdempotencyKey(String idempotencyKey, String partnerId) {
        return idempotencyKeyRepository.findByIdempotencyKeyAndPartnerId(idempotencyKey, partnerId);
    }

    /**
     * Creates a payment, optionally stores idempotency key, debits wallet, and publishes
     * outbox events. Transactional. On insufficient balance, returns FAILED (does not throw)
     * so the transaction commits and payment.failed can be consumed by offer-service.
     *
     * @param request      payment details; offerId required
     * @param ownerType    e.g. PARTNER or BUYER
     * @param ownerId      owner identifier (used with partnerId for idempotency lookup)
     * @param partnerId    may be null for buyer flow; empty string stored for backward compatibility
     * @param idempotencyKey optional; when set, stored for owner (partnerId or ownerId)
     * @return created payment response; status CAPTURED or FAILED after wallet debit
     * @throws IllegalArgumentException if offerId is null
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String ownerType, String ownerId, String partnerId, String idempotencyKey) {
        Payment payment = new Payment();
        payment.setPartnerId(partnerId != null ? partnerId : "");
        payment.setOwnerType(ownerType);
        payment.setOwnerId(ownerId);

        if (request.getOfferId() == null) {
            throw new IllegalArgumentException("offerId is required");
        }
        payment.setOfferId(request.getOfferId());
        payment.setReferenceType(request.getReferenceType() != null ? request.getReferenceType() : "OFFER_LISTING");
        payment.setReferenceId(request.getReferenceId());
        payment.setAmountCents(request.getAmountCents());
        payment.setCurrency(request.getCurrency());
        payment.setStatus("INITIATED");

        Payment savedPayment = paymentRepository.save(payment);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey key = new IdempotencyKey();
            key.setIdempotencyKey(idempotencyKey);
            key.setPartnerId(partnerId != null ? partnerId : ownerId);
            key.setPaymentId(savedPayment.getId());
            idempotencyKeyRepository.save(key);
        }

        boolean debited = walletService.debit(ownerType, ownerId,
            savedPayment.getAmountCents(),
            savedPayment.getReferenceType(),
            savedPayment.getReferenceId());

        if (debited) {
            savedPayment.setStatus("CAPTURED");
            savedPayment.setProviderPaymentId("wallet");
            paymentRepository.save(savedPayment);
            publishPaymentCapturedEvent(savedPayment);
            paymentMetrics.incrementPaymentsCaptured();
        } else {
            savedPayment.setStatus("FAILED");
            savedPayment.setProviderPaymentId(null);
            paymentRepository.save(savedPayment);
            publishPaymentFailedEvent(savedPayment, "Insufficient wallet balance");
            paymentMetrics.incrementPaymentsFailed();
        }

        publishPaymentInitiatedEvent(savedPayment);
        paymentMetrics.incrementPaymentsCreated();

        return toResponse(savedPayment);
    }

    /**
     * Returns a payment by id if it belongs to the given owner. Read-only.
     *
     * @param id      payment id
     * @param ownerId must match payment's ownerId
     * @return payment response
     * @throws RuntimeException if not found or owner mismatch
     */
    public PaymentResponse getPaymentById(UUID id, String ownerId) {
        Payment payment = paymentRepository.findById(id)
            .filter(p -> ownerId.equals(p.getOwnerId()))
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        return toResponse(payment);
    }

    /**
     * Lists payments for a partner, newest first. Read-only.
     *
     * @param partnerId partner id; null/blank returns empty list
     * @return list of payment responses
     */
    public List<PaymentResponse> getPaymentsByPartnerId(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) return List.of();
        List<Payment> payments = paymentRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
        return payments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Builds timeline of events for a payment, scoped to owner. Read-only.
     *
     * @param id      payment id
     * @param ownerId must match payment's ownerId
     * @return ordered list of timeline events
     * @throws RuntimeException if payment not found or owner mismatch
     */
    public List<TimelineEventResponse> getPaymentTimeline(UUID id, String ownerId) {
        Payment payment = paymentRepository.findById(id)
            .filter(p -> ownerId.equals(p.getOwnerId()))
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        List<TimelineEventResponse> timeline = new ArrayList<>();
        timeline.add(new TimelineEventResponse(
            payment.getCreatedAt(),
            "PAYMENT_INITIATED",
            "Payment created for offer " + payment.getOfferId()
        ));
        List<ProviderEvent> providerEvents = providerEventRepository.findByPaymentIdOrderByReceivedAtAsc(id);
        for (ProviderEvent event : providerEvents) {
            timeline.add(new TimelineEventResponse(
                event.getReceivedAt(),
                "PROVIDER_WEBHOOK_RECEIVED",
                event.getEventType()
            ));
        }
        if (!"INITIATED".equals(payment.getStatus())) {
            timeline.add(new TimelineEventResponse(
                payment.getUpdatedAt(),
                "PAYMENT_STATUS_CHANGED",
                "Status changed to " + payment.getStatus()
            ));
        }
        
        return timeline;
    }
    
    /**
     * Processes a provider webhook: idempotent by providerEventId, updates payment status,
     * and publishes payment.captured or payment.failed via outbox. Transactional.
     *
     * @param request        webhook payload
     * @param providerEventId idempotency key; duplicate events are ignored
     * @throws RuntimeException if payment not found for providerPaymentId
     */
    @Transactional
    public void handleWebhook(ProviderWebhookRequest request, String providerEventId) {
        paymentMetrics.incrementWebhooksReceived();
        if (providerEventRepository.existsById(providerEventId)) {
            logger.info("Webhook already processed: providerEventId={}", providerEventId);
            return;
        }
        Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(request.getProviderPaymentId());
        if (paymentOpt.isEmpty()) {
            logger.warn("Payment not found for providerPaymentId: {}", request.getProviderPaymentId());
            throw new RuntimeException("Payment not found");
        }
        Payment payment = paymentOpt.get();
        ProviderEvent event = new ProviderEvent();
        event.setProviderEventId(providerEventId);
        event.setProviderPaymentId(request.getProviderPaymentId());
        event.setPaymentId(payment.getId());
        event.setEventType("payment." + request.getStatus().toLowerCase());
        event.setReceivedAt(request.getOccurredAt());
        providerEventRepository.save(event);
        String newStatus = request.getStatus();
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        if ("CAPTURED".equals(newStatus)) {
            publishPaymentCapturedEvent(payment);
            paymentMetrics.incrementPaymentsCaptured();
        } else if ("FAILED".equals(newStatus)) {
            publishPaymentFailedEvent(payment, "Provider reported payment failed");
            paymentMetrics.incrementPaymentsFailed();
        }
        
        logger.info("Processed webhook: providerEventId={}, paymentId={}, status={}", 
                   providerEventId, payment.getId(), newStatus);
    }
    
    private void publishPaymentInitiatedEvent(Payment payment) {
        try {
            PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                payment.getId(),
                payment.getPartnerId(),
                payment.getOfferId(),
                payment.getAmountCents(),
                payment.getCurrency()
            );
            outboxService.publishEvent("PAYMENT", payment.getId(), PAYMENT_INITIATED_TOPIC, event);
            logger.info("Queued payment.initiated event to outbox for paymentId: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to queue payment.initiated event to outbox for paymentId: {}", payment.getId(), e);
        }
    }
    
    private void publishPaymentCapturedEvent(Payment payment) {
        try {
            PaymentCapturedEvent event = new PaymentCapturedEvent(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                payment.getId(),
                payment.getPartnerId(),
                payment.getOfferId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getProviderPaymentId(),
                payment.getReferenceType(),
                payment.getReferenceId(),
                payment.getOwnerType(),
                payment.getOwnerId()
            );
            outboxService.publishEvent("PAYMENT", payment.getId(), PAYMENT_CAPTURED_TOPIC, event);
            logger.info("Queued payment.captured event to outbox for paymentId: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to queue payment.captured event to outbox for paymentId: {}", payment.getId(), e);
        }
    }
    
    private void publishPaymentFailedEvent(Payment payment, String reason) {
        try {
            PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                payment.getId(),
                payment.getPartnerId(),
                payment.getOfferId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getProviderPaymentId(),
                reason,
                payment.getReferenceType(),
                payment.getReferenceId(),
                payment.getOwnerType(),
                payment.getOwnerId()
            );
            outboxService.publishEvent("PAYMENT", payment.getId(), PAYMENT_FAILED_TOPIC, event);
            logger.info("Queued payment.failed event to outbox for paymentId: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to queue payment.failed event to outbox for paymentId: {}", payment.getId(), e);
        }
    }
    
    private static final java.time.OffsetDateTime EPOCH_START = java.time.OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
    private static final java.time.OffsetDateTime FAR_FUTURE = java.time.OffsetDateTime.of(2100, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);

    /**
     * Internal/ops search; not scoped by partnerId. Reads from DB only.
     */
    public List<PaymentResponse> searchPayments(UUID paymentId, UUID offerId, String partnerId,
                                                 OffsetDateTime createdFrom, OffsetDateTime createdTo, String status) {
        List<PaymentResponse> responses;
        if (paymentId != null) {
            Optional<Payment> payment = paymentRepository.findById(paymentId);
            responses = payment.map(p -> List.of(toResponse(p))).orElse(List.of());
        } else {
            OffsetDateTime from = createdFrom != null ? createdFrom : EPOCH_START;
            OffsetDateTime to = createdTo != null ? createdTo : FAR_FUTURE;
            List<Payment> payments = paymentRepository.searchPayments(
                offerId,
                (partnerId != null && !partnerId.isBlank()) ? partnerId : null,
                (status != null && !status.isBlank()) ? status : null,
                from,
                to,
                PageRequest.of(0, 100)
            );
            responses = payments.stream().map(this::toResponse).collect(Collectors.toList());
        }
        return responses;
    }

    /**
     * Ops-only: returns payment by id without owner scoping. Read-only.
     *
     * @param id payment id
     * @return payment response
     * @throws RuntimeException if not found
     */
    public PaymentResponse getPaymentByIdForOps(UUID id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        return toResponse(payment);
    }

    /**
     * Refunds a captured payment: credits the owner's wallet and sets status to REFUNDED.
     * Only CAPTURED payments with referenceType ORDER_PURCHASE are allowed. Transactional.
     *
     * @param paymentId payment to refund
     * @return updated payment response
     * @throws RuntimeException if payment not found, not CAPTURED, or not ORDER_PURCHASE
     */
    @Transactional
    public PaymentResponse refundByPaymentId(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        if (!"CAPTURED".equals(payment.getStatus())) {
            throw new RuntimeException("Only captured payments can be refunded; current status: " + payment.getStatus());
        }
        if (!"ORDER_PURCHASE".equals(payment.getReferenceType())) {
            throw new RuntimeException("Only order purchases can be refunded via this flow");
        }
        walletService.credit(
            payment.getOwnerType(),
            payment.getOwnerId(),
            payment.getAmountCents(),
            "ORDER_REFUND",
            payment.getId()
        );
        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);
        logger.info("Refunded payment: paymentId={}, ownerId={}, amountCents={}", paymentId, payment.getOwnerId(), payment.getAmountCents());
        return toResponse(payment);
    }

    /**
     * Ops-only: returns timeline events for a payment without owner scoping. Read-only.
     *
     * @param id payment id
     * @return ordered list of timeline events
     * @throws RuntimeException if payment not found
     */
    public List<TimelineEventResponse> getPaymentTimelineForOps(UUID id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        List<TimelineEventResponse> timeline = new ArrayList<>();
        timeline.add(new TimelineEventResponse(
            payment.getCreatedAt(),
            "PAYMENT_INITIATED",
            "Payment created for offer " + payment.getOfferId()
        ));
        List<ProviderEvent> providerEvents = providerEventRepository.findByPaymentIdOrderByReceivedAtAsc(id);
        for (ProviderEvent event : providerEvents) {
            timeline.add(new TimelineEventResponse(
                event.getReceivedAt(),
                "PROVIDER_WEBHOOK_RECEIVED",
                "X-Provider-Event-Id=" + event.getProviderEventId() + ", eventType=" + event.getEventType()
            ));
        }
        if ("CAPTURED".equals(payment.getStatus())) {
            timeline.add(new TimelineEventResponse(
                payment.getUpdatedAt(),
                "PAYMENT_CAPTURED",
                "Payment captured"
            ));
        } else if ("FAILED".equals(payment.getStatus())) {
            timeline.add(new TimelineEventResponse(
                payment.getUpdatedAt(),
                "PAYMENT_FAILED",
                "Payment failed"
            ));
        }
        timeline.sort((a, b) -> a.getAt().compareTo(b.getAt()));
        return timeline;
    }

    /**
     * Re-applies the latest provider webhook: updates payment status and republishes
     * outbox event. Does not insert a new provider_event row. Idempotent for status update.
     * Transactional.
     *
     * @param paymentId payment to retry
     * @return updated payment response
     * @throws RuntimeException if payment or provider events not found
     */
    @Transactional
    public PaymentResponse retryLatestWebhook(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        Optional<ProviderEvent> latestEventOpt = providerEventRepository.findTop1ByPaymentIdOrderByReceivedAtDesc(paymentId);
        if (latestEventOpt.isEmpty()) {
            throw new RuntimeException("No provider events found for this payment");
        }
        ProviderEvent latestEvent = latestEventOpt.get();
        String status = extractStatusFromEventType(latestEvent.getEventType());
        payment.setStatus(status);
        paymentRepository.save(payment);
        if ("CAPTURED".equals(status)) {
            publishPaymentCapturedEvent(payment);
        } else if ("FAILED".equals(status)) {
            publishPaymentFailedEvent(payment, "Retried webhook processing");
        }
        
        logger.info("Retried latest webhook for payment: paymentId={}, status={}", paymentId, status);
        
        return toResponse(payment);
    }
    
    private String extractStatusFromEventType(String eventType) {
        if (eventType != null && eventType.contains("captured")) {
            return "CAPTURED";
        }
        if (eventType != null && eventType.contains("failed")) {
            return "FAILED";
        }
        return "INITIATED";
    }
    
    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setPartnerId(payment.getPartnerId());
        response.setOfferId(payment.getOfferId());
        response.setAmountCents(payment.getAmountCents());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setReferenceType(payment.getReferenceType());
        response.setProviderPaymentId(payment.getProviderPaymentId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}
