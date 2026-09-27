package com.joborch.producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joborch.common.domain.JobStatus;
import com.joborch.common.domain.JobStateMachine;
import com.joborch.common.messaging.JobMessage;
import com.joborch.producer.config.RabbitMQConfig;
import com.joborch.producer.domain.Job;
import com.joborch.producer.domain.OutboxEvent;
import com.joborch.producer.dto.CreateJobRequest;
import com.joborch.producer.dto.JobResponse;
import com.joborch.producer.exception.JobNotFoundException;
import com.joborch.producer.repository.JobRepository;
import com.joborch.producer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobResponse createJob(CreateJobRequest request) {

        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        return jobRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingJob -> {
                    log.info("Duplicate request detected for idempotencyKey={}, returning existing job {}",
                            idempotencyKey, existingJob.getId());
                    return toResponse(existingJob);
                })
                .orElseGet(() -> createNewJob(request, idempotencyKey));
    }

    private JobResponse createNewJob(CreateJobRequest request, String idempotencyKey) {

        Job job = Job.builder()
                .idempotencyKey(idempotencyKey)
                .name(request.getName())
                .description(request.getDescription())
                .jobType(request.getJobType())
                .priority(request.getPriority())
                .payload(request.getPayload())
                .scheduledAt(request.getScheduledAt())
                .status(JobStatus.PENDING)
                .maxAttempts(request.getMaxAttempts())
                .build();

        job = jobRepository.save(job);
        log.info("Job persisted: id={}, idempotencyKey={}", job.getId(), idempotencyKey);

        if (job.getScheduledAt() == null) {
            writeOutboxEvent(job);
            job.setStatus(JobStatus.QUEUED);

        }

        return toResponse(job);
    }

    private void writeOutboxEvent(Job job) {
        try {
            JobMessage message = JobMessage.forFirstAttempt(
                    job.getId(), job.getJobType(), job.getPriority(), job.getPayload());

            String payloadJson = objectMapper.writeValueAsString(message);

            OutboxEvent event = OutboxEvent.builder()
                    .jobId(job.getId())
                    .exchangeName(RabbitMQConfig.JOBS_EXCHANGE)
                    .routingKey(RabbitMQConfig.ROUTING_JOBS)
                    .payload(payloadJson)
                    .build();

            outboxEventRepository.save(event);
            log.debug("Outbox event written for job {}", job.getId());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JobMessage for outbox", e);
        }
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID id) {
        return jobRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new JobNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public JobStatus getJobStatus(UUID id) {
        return jobRepository.findById(id)
                .map(Job::getStatus)
                .orElseThrow(() -> new JobNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .idempotencyKey(job.getIdempotencyKey())
                .name(job.getName())
                .description(job.getDescription())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .priority(job.getPriority())
                .payload(job.getPayload())
                .attemptCount(job.getAttemptCount())
                .maxAttempts(job.getMaxAttempts())
                .errorMessage(job.getErrorMessage())
                .scheduledAt(job.getScheduledAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
