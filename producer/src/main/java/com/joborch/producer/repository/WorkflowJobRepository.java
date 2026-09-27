package com.joborch.producer.repository;

import com.joborch.producer.domain.WorkflowJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowJobRepository extends JpaRepository<WorkflowJob, UUID> {

    List<WorkflowJob> findByWorkflowId(UUID workflowId);

    @Query("""
        SELECT wj FROM WorkflowJob wj
        WHERE wj.workflow.id = :workflowId
        AND wj.status = com.joborch.common.domain.JobStatus.COMPLETED
        """)
    List<WorkflowJob> findCompletedByWorkflowId(@Param("workflowId") UUID workflowId);
}
