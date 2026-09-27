package com.joborch.producer.dto;

import com.joborch.common.domain.JobPriority;
import com.joborch.common.domain.JobType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {

    @NotBlank
    private String name;
    private String description;

    @NotEmpty
    @Valid
    private List<StepDefinition> steps;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDefinition {
        @NotBlank
        private String stepName;

        @NotNull
        private JobType jobType;

        private JobPriority priority = JobPriority.NORMAL;
        private Map<String, String> payload = new HashMap<>();

        private List<String> dependsOn = List.of();
    }
}
