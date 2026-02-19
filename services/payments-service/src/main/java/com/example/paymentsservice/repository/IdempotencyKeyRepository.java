package com.example.paymentsservice.repository;

import com.example.paymentsservice.entity.IdempotencyKey;
import com.example.paymentsservice.entity.IdempotencyKeyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKeyId> {
    
    Optional<IdempotencyKey> findByIdempotencyKeyAndPartnerId(String idempotencyKey, String partnerId);
}
