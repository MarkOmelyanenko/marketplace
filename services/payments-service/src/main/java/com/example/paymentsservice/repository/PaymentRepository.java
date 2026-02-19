package com.example.paymentsservice.repository;

import com.example.paymentsservice.entity.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByIdAndPartnerId(UUID id, String partnerId);
    
    List<Payment> findByPartnerIdOrderByCreatedAtDesc(String partnerId);
    
    List<Payment> findTop50ByPartnerIdOrderByCreatedAtDesc(String partnerId);
    
    List<Payment> findByOfferId(UUID offerId);

    List<Payment> findTop100ByOrderByCreatedAtDesc();

    List<Payment> findTop50ByOfferIdOrderByCreatedAtDesc(UUID offerId);
    
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
    
    List<Payment> findByStatusAndProviderPaymentIdIsNull(String status);

    @Query("SELECT p FROM Payment p WHERE (:offerId IS NULL OR p.offerId = :offerId) AND (:partnerId IS NULL OR p.partnerId = :partnerId) AND (:status IS NULL OR p.status = :status) AND p.createdAt >= :from AND p.createdAt <= :to ORDER BY p.createdAt DESC")
    List<Payment> searchPayments(@Param("offerId") UUID offerId, @Param("partnerId") String partnerId, @Param("status") String status, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, Pageable pageable);
}
