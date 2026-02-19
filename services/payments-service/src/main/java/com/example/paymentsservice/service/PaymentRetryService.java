package com.example.paymentsservice.service;

import com.example.paymentsservice.client.ProviderClient;
import com.example.paymentsservice.dto.ProviderCreatePaymentRequest;
import com.example.paymentsservice.entity.Payment;
import com.example.paymentsservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled job that retries INITIATED payments without provider_payment_id by calling
 * the provider and updating the payment. Runs every 30 seconds.
 */
@Service
public class PaymentRetryService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRetryService.class);

    private final PaymentRepository paymentRepository;
    private final ProviderClient providerClient;

    public PaymentRetryService(PaymentRepository paymentRepository, ProviderClient providerClient) {
        this.paymentRepository = paymentRepository;
        this.providerClient = providerClient;
    }

    /**
     * Finds INITIATED payments with null provider_payment_id and attempts to create payment at provider.
     * Transactional; performs DB updates on success.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void retryPendingPayments() {
        try {
            List<Payment> pendingPayments = paymentRepository.findByStatusAndProviderPaymentIdIsNull("INITIATED");
            
            if (pendingPayments.isEmpty()) {
                return;
            }
            
            logger.info("Found {} payments to retry", pendingPayments.size());
            
            for (Payment payment : pendingPayments) {
                try {
                    ProviderCreatePaymentRequest providerRequest = new ProviderCreatePaymentRequest(
                        payment.getId(),
                        payment.getAmountCents(),
                        payment.getCurrency()
                    );
                    
                    providerClient.createPayment(providerRequest)
                        .subscribe(
                            response -> {
                                payment.setProviderPaymentId(response.getProviderPaymentId());
                                paymentRepository.save(payment);
                                logger.info("Retried and updated payment with providerPaymentId: paymentId={}, providerPaymentId={}", 
                                           payment.getId(), response.getProviderPaymentId());
                            },
                            error -> {
                                logger.warn("Retry failed for payment: paymentId={}", payment.getId(), error);
                            }
                        );
                } catch (Exception e) {
                    logger.error("Error retrying payment: paymentId={}", payment.getId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in payment retry job", e);
        }
    }
}
