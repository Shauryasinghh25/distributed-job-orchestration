package com.joborch.producer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "workflow_job_dependencies",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_wf_dep",
        columnNames = {"required_job_id", "dependent_job_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowJobDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_job_id", nullable = false)
    private WorkflowJob requiredJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_job_id", nullable = false)
    private WorkflowJob dependentJob;
}
