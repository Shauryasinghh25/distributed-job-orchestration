package com.joborch.producer.dto;

import com.joborch.common.domain.JobPriority;
import com.joborch.common.domain.JobStatus;
import com.joborch.common.domain.JobType;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private UUID id;
    private String idempotencyKey;
    private String name;
    private String description;
    private JobType jobType;
    private JobStatus status;
    private JobPriority priority;
    private Map<String, String> payload;
    private int attemptCount;
    private int maxAttempts;
    private String errorMessage;
    private Instant scheduledAt;
    private Instant createdAt;
    private Instant updatedAt;
}
