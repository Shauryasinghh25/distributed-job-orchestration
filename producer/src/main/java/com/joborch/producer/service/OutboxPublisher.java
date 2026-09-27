package com.joborch.producer.service;

import com.joborch.producer.domain.OutboxEvent;
import com.joborch.producer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final int MAX_PUBLISH_ATTEMPTS = 5;

    @Scheduled(fixedDelayString = "${joborch.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();

        if (!pending.isEmpty()) {
            log.debug("Outbox publisher found {} pending events", pending.size());
        }

        for (OutboxEvent event : pending) {
            if (event.getPublishAttempts() >= MAX_PUBLISH_ATTEMPTS) {
                log.error("Outbox event {} exceeded max publish attempts, skipping. Manual intervention required.",
                        event.getId());
                continue;
            }

            try {

                MessageProperties props = new MessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setContentEncoding(StandardCharsets.UTF_8.name());
                byte[] body = event.getPayload().getBytes(StandardCharsets.UTF_8);
                Message rawMessage = new Message(body, props);

                rabbitTemplate.send(
                        event.getExchangeName(),
                        event.getRoutingKey(),
                        rawMessage);
                outboxEventRepository.markPublished(event.getId());
                log.info("Outbox event {} published for job {}", event.getId(), event.getJobId());

            } catch (AmqpException e) {
                outboxEventRepository.incrementAttempts(event.getId());
                log.warn("Failed to publish outbox event {} (attempt {}): {}",
                        event.getId(), event.getPublishAttempts() + 1, e.getMessage());
            }
        }
    }
}
