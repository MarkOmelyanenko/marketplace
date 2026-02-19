package com.example.paymentsservice.service;

import com.example.paymentsservice.dto.CreatePaymentRequest;
import com.example.paymentsservice.entity.IdempotencyKey;
import com.example.paymentsservice.entity.Payment;
import com.example.paymentsservice.repository.IdempotencyKeyRepository;
import com.example.paymentsservice.repository.PaymentRepository;
import com.example.paymentsservice.repository.ProviderEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String PARTNER_ID = "partner-1";
    private static final String OWNER_ID = "buyer-1";
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID OFFER_ID = UUID.randomUUID();

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private ProviderEventRepository providerEventRepository;
    @Mock
    private com.example.paymentsservice.client.ProviderClient providerClient;
    @Mock
    private WalletService walletService;
    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private OutboxService outboxService;
    @Mock
    private com.example.paymentsservice.metrics.PaymentMetrics paymentMetrics;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                idempotencyKeyRepository,
                providerEventRepository,
                providerClient,
                walletService,
                kafkaTemplate,
                outboxService,
                paymentMetrics
        );
    }

    @Test
    void findIdempotencyKey_returnsEmptyWhenNotFound() {
        when(idempotencyKeyRepository.findByIdempotencyKeyAndPartnerId("key-1", PARTNER_ID))
                .thenReturn(Optional.empty());

        Optional<IdempotencyKey> result = paymentService.findIdempotencyKey("key-1", PARTNER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void findIdempotencyKey_returnsKeyWhenFound() {
        IdempotencyKey key = new IdempotencyKey();
        key.setIdempotencyKey("key-1");
        key.setPartnerId(PARTNER_ID);
        key.setPaymentId(PAYMENT_ID);
        when(idempotencyKeyRepository.findByIdempotencyKeyAndPartnerId("key-1", PARTNER_ID))
                .thenReturn(Optional.of(key));

        Optional<IdempotencyKey> result = paymentService.findIdempotencyKey("key-1", PARTNER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getPaymentId()).isEqualTo(PAYMENT_ID);
    }

    @Test
    void createPayment_throwsWhenOfferIdNull() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOfferId(null);
        request.setAmountCents(999);
        request.setCurrency("USD");

        assertThatThrownBy(() -> paymentService.createPayment(request, "BUYER", OWNER_ID, PARTNER_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offerId is required");
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
