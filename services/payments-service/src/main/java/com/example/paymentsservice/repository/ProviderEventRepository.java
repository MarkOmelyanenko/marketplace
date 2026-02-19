package com.example.paymentsservice.repository;

import com.example.paymentsservice.entity.ProviderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderEventRepository extends JpaRepository<ProviderEvent, String> {
    
    List<ProviderEvent> findByPaymentIdOrderByReceivedAtAsc(UUID paymentId);
    
    Optional<ProviderEvent> findTop1ByPaymentIdOrderByReceivedAtDesc(UUID paymentId);
}
