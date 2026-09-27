package com.joborch.producer.service;

import com.joborch.common.domain.JobStatus;
import com.joborch.producer.domain.Job;
import com.joborch.producer.repository.JobRepository;
import com.joborch.producer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobProcessor {

    private final JobRepository jobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JobService jobService;

    @Scheduled(fixedDelayString = "${joborch.scheduling.poll-interval-ms:5000}")
    @Transactional
    public void processScheduledJobs() {
        List<Job> readyJobs = jobRepository.findReadyToQueue(Instant.now());
        if (readyJobs.isEmpty()) return;

        log.info("Found {} job(s) ready to queue", readyJobs.size());

        for (Job job : readyJobs) {
            try {

                jobRepository.updateStatus(job.getId(), JobStatus.QUEUED);

                writeOutboxEvent(job);
                log.info("Scheduled job {} enqueued", job.getId());
            } catch (Exception e) {
                log.error("Failed to enqueue scheduled job {}: {}", job.getId(), e.getMessage());
            }
        }
    }

    private void writeOutboxEvent(Job job) {

        com.joborch.common.messaging.JobMessage message =
                com.joborch.common.messaging.JobMessage.forFirstAttempt(
                        job.getId(), job.getJobType(), job.getPriority(), job.getPayload());
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .writeValueAsString(message);

            outboxEventRepository.save(
                    com.joborch.producer.domain.OutboxEvent.builder()
                            .jobId(job.getId())
                            .exchangeName(com.joborch.producer.config.RabbitMQConfig.JOBS_EXCHANGE)
                            .routingKey(com.joborch.producer.config.RabbitMQConfig.ROUTING_JOBS)
                            .payload(json)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to write outbox event for scheduled job", e);
        }
    }
}
