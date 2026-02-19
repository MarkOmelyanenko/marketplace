package com.example.paymentsservice.integration;

import com.example.paymentsservice.controller.WebhookController;
import com.example.paymentsservice.dto.ProviderWebhookRequest;
import com.example.paymentsservice.entity.Payment;
import com.example.paymentsservice.entity.ProviderEvent;
import com.example.paymentsservice.repository.PaymentRepository;
import com.example.paymentsservice.repository.ProviderEventRepository;
import com.example.paymentsservice.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WebhookIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("test_payment_db")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("provider.webhook-secret", () -> "test-secret");
    }
    
    @Autowired
    private WebhookController webhookController;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ProviderEventRepository providerEventRepository;
    
    @Autowired
    private WebhookService webhookService;
    
    private Payment testPayment;
    private String providerEventId;
    
    @BeforeEach
    void setUp() {
        // create a test payment
        testPayment = new Payment();
        testPayment.setId(UUID.randomUUID());
        testPayment.setPartnerId("partner-123");
        testPayment.setOfferId(UUID.randomUUID());
        testPayment.setAmountCents(10000);
        testPayment.setCurrency("USD");
        testPayment.setStatus("INITIATED");
        testPayment.setProviderPaymentId("psp_12345");
        testPayment.setCreatedAt(OffsetDateTime.now());
        testPayment.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(testPayment);
        
        providerEventId = UUID.randomUUID().toString();
    }
    
    @Test
    void shouldProcessWebhookWithValidSignature() throws Exception {
        // given
        ProviderWebhookRequest request = new ProviderWebhookRequest();
        request.setProviderPaymentId("psp_12345");
        request.setStatus("CAPTURED");
        request.setAmountCents(10000);
        request.setCurrency("USD");
        request.setOccurredAt(OffsetDateTime.now());
        
        String jsonBody = """
            {
                "providerPaymentId": "psp_12345",
                "status": "CAPTURED",
                "amountCents": 10000,
                "currency": "USD",
                "occurredAt": "%s"
            }
            """.formatted(request.getOccurredAt());
        
        String signature = computeSignature(jsonBody, "test-secret");
        
        // when
        ResponseEntity<Map<String, String>> response = webhookController.handleProviderWebhook(
            request, providerEventId, signature, null);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // verify payment status updated
        Payment updated = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("CAPTURED");
        
        // verify provider event stored
        assertThat(providerEventRepository.existsById(providerEventId)).isTrue();
    }
    
    @Test
    void shouldRejectWebhookWithInvalidSignature() throws Exception {
        // given
        ProviderWebhookRequest request = new ProviderWebhookRequest();
        request.setProviderPaymentId("psp_12345");
        request.setStatus("CAPTURED");
        request.setAmountCents(10000);
        request.setCurrency("USD");
        request.setOccurredAt(OffsetDateTime.now());
        
        String invalidSignature = "invalid-signature";
        
        // when
        ResponseEntity<Map<String, String>> response = webhookController.handleProviderWebhook(
            request, providerEventId, invalidSignature, null);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    void shouldBeIdempotentWhenProcessingSameWebhookTwice() throws Exception {
        // given
        ProviderWebhookRequest request = new ProviderWebhookRequest();
        request.setProviderPaymentId("psp_12345");
        request.setStatus("CAPTURED");
        request.setAmountCents(10000);
        request.setCurrency("USD");
        request.setOccurredAt(OffsetDateTime.now());
        
        String jsonBody = """
            {
                "providerPaymentId": "psp_12345",
                "status": "CAPTURED",
                "amountCents": 10000,
                "currency": "USD",
                "occurredAt": "%s"
            }
            """.formatted(request.getOccurredAt());
        
        String signature = computeSignature(jsonBody, "test-secret");
        
        // when - first call
        ResponseEntity<Map<String, String>> response1 = webhookController.handleProviderWebhook(
            request, providerEventId, signature, null);
        
        // then - first call succeeds
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // when - second call with same event ID
        ResponseEntity<Map<String, String>> response2 = webhookController.handleProviderWebhook(
            request, providerEventId, signature, null);
        
        // then - second call also succeeds (idempotent)
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // verify only one provider event stored
        assertThat(providerEventRepository.findAll()).hasSize(1);
    }
    
    private String computeSignature(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
