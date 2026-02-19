package com.example.offerservice.repository;

import com.example.offerservice.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    
    Optional<IdempotencyKey> findByIdempotencyKeyAndPartnerId(String idempotencyKey, String partnerId);
}
