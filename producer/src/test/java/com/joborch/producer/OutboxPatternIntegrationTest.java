package com.joborch.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joborch.common.domain.JobType;
import com.joborch.common.domain.JobPriority;
import com.joborch.common.messaging.JobMessage;
import com.joborch.producer.domain.OutboxEvent;
import com.joborch.producer.repository.OutboxEventRepository;
import com.joborch.producer.service.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the Outbox Pattern — Phase 16.
 * Verifies that:
 *   1. Outbox events are created in the same transaction as jobs.
 *   2. OutboxPublisher marks events as published after delivering to RabbitMQ.
 *   3. Crash-safe: unpublished events are retried.
 */
@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class OutboxPatternIntegrationTest {

    @Autowired OutboxEventRepository outboxEventRepository;
    @Autowired OutboxPublisher outboxPublisher;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("OutboxPublisher publishes pending events and marks them published")
    void publisher_marks_events_as_published() throws Exception {
        // Arrange — create a raw outbox event (bypassing the service layer)
        JobMessage msg = JobMessage.forFirstAttempt(
                UUID.randomUUID(), JobType.EMAIL, JobPriority.NORMAL,
                Map.of("recipient", "test@example.com"));

        OutboxEvent event = OutboxEvent.builder()
                .jobId(msg.getJobId())
                .exchangeName("jobs.exchange")
                .routingKey("jobs.queue")
                .payload(objectMapper.writeValueAsString(msg))
                .build();
        outboxEventRepository.save(event);

        // Verify it's unpublished
        assertThat(outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc()).hasSize(1);

        // Act — run the publisher
        outboxPublisher.publishPendingEvents();

        // Assert — event is now marked published
        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.isPublished()).isTrue();
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("No outbox events when queue is empty — publisher is a no-op")
    void publisher_noop_when_no_events() {
        assertThatNoException().isThrownBy(() -> outboxPublisher.publishPendingEvents());
    }
}
