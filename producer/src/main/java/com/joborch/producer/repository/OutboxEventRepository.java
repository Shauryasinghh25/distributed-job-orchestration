package com.joborch.producer.repository;

import com.joborch.producer.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.published = true, e.publishedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    int markPublished(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.publishAttempts = e.publishAttempts + 1 WHERE e.id = :id")
    int incrementAttempts(@Param("id") UUID id);
}
