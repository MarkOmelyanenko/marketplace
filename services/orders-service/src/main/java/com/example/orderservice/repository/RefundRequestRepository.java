package com.example.orderservice.repository;

import com.example.orderservice.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    Optional<RefundRequest> findByOrderIdAndStatus(UUID orderId, String status);

    List<RefundRequest> findByOrderIdAndBuyerIdOrderByCreatedAtDesc(UUID orderId, String buyerId);

    List<RefundRequest> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<RefundRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<RefundRequest> findAllByOrderByCreatedAtDesc();
}
