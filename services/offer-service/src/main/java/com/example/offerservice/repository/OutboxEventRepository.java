package com.example.offerservice.repository;

import com.example.offerservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.status IN ('NEW', 'FAILED') AND e.attempts < 10 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsForPublishing();
    
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'SENDING' WHERE e.id IN :ids AND e.status IN ('NEW', 'FAILED')")
    int markAsSending(@Param("ids") List<UUID> ids);
}
