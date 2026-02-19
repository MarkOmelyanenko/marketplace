package com.example.offerservice.repository;

import com.example.offerservice.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID> {
    
    List<Offer> findByPartnerIdOrderByCreatedAtDesc(String partnerId);
    
    Optional<Offer> findByIdAndPartnerId(UUID id, String partnerId);
    
    List<Offer> findTop50ByStatusOrderByCreatedAtDesc(String status);
}
