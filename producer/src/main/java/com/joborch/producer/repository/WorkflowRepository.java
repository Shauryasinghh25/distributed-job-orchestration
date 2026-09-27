package com.joborch.producer.repository;

import com.joborch.producer.domain.Workflow;
import com.joborch.producer.domain.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findByStatus(WorkflowStatus status);
}
