package com.joborch.producer.dto;

import com.joborch.common.domain.JobPriority;
import com.joborch.common.domain.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobRequest {

    @NotBlank(message = "Job name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "Job type is required")
    private JobType jobType;

    @Builder.Default
    private JobPriority priority = JobPriority.NORMAL;

    @Builder.Default
    private Map<String, String> payload = new HashMap<>();

    private String idempotencyKey;

    private Instant scheduledAt;

    @Builder.Default
    private int maxAttempts = 3;
}
