package com.joborch.producer.dto;

import com.joborch.producer.domain.WorkflowStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {
    private UUID id;
    private String name;
    private String description;
    private WorkflowStatus status;
    private Instant createdAt;
}
