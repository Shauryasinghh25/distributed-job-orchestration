package com.joborch.producer.service;

import com.joborch.common.domain.JobStatus;
import com.joborch.producer.domain.*;
import com.joborch.producer.dto.CreateWorkflowRequest;
import com.joborch.producer.dto.WorkflowResponse;
import com.joborch.producer.repository.WorkflowJobRepository;
import com.joborch.producer.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowJobRepository workflowJobRepository;
    private final JobService jobService;

    @Transactional
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        Workflow workflow = Workflow.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(WorkflowStatus.RUNNING)
                .build();
        workflow = workflowRepository.save(workflow);

        Map<String, WorkflowJob> stepMap = new HashMap<>();
        for (CreateWorkflowRequest.StepDefinition step : request.getSteps()) {
            WorkflowJob wj = WorkflowJob.builder()
                    .workflow(workflow)
                    .stepName(step.getStepName())
                    .jobType(step.getJobType())
                    .priority(step.getPriority())
                    .payload(step.getPayload())
                    .status(JobStatus.PENDING)
                    .build();
            wj = workflowJobRepository.save(wj);
            stepMap.put(step.getStepName(), wj);
        }

        for (CreateWorkflowRequest.StepDefinition step : request.getSteps()) {
            WorkflowJob dependent = stepMap.get(step.getStepName());
            for (String requiredName : step.getDependsOn()) {
                WorkflowJob required = stepMap.get(requiredName);
                if (required == null) {
                    throw new IllegalArgumentException(
                            "Step '" + step.getStepName() + "' depends on unknown step '" + requiredName + "'");
                }
                WorkflowJobDependency dep = WorkflowJobDependency.builder()
                        .requiredJob(required)
                        .dependentJob(dependent)
                        .build();
                dependent.getDependencies().add(dep);
            }
            workflowJobRepository.save(dependent);
        }

        validateNoCycles(new ArrayList<>(stepMap.values()));

        advanceWorkflow(workflow.getId());

        return toResponse(workflowRepository.findById(workflow.getId()).orElseThrow());
    }

    @Transactional
    public void onWorkflowJobCompleted(UUID workflowId, UUID workflowJobId) {

        WorkflowJob wj = workflowJobRepository.findById(workflowJobId).orElse(null);
        if (wj == null) return;
        wj.setStatus(JobStatus.COMPLETED);
        workflowJobRepository.save(wj);

        advanceWorkflow(workflowId);
    }

    @Transactional
    public void onWorkflowJobFailed(UUID workflowId, UUID workflowJobId) {
        WorkflowJob wj = workflowJobRepository.findById(workflowJobId).orElse(null);
        if (wj == null) return;
        wj.setStatus(JobStatus.DEAD_LETTERED);
        workflowJobRepository.save(wj);

        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        workflow.setStatus(WorkflowStatus.FAILED);
        workflowRepository.save(workflow);

        cancelDependents(workflowId, workflowJobId);
        log.warn("Workflow {} failed at step {}", workflowId, wj.getStepName());
    }

    private void advanceWorkflow(UUID workflowId) {
        List<WorkflowJob> allJobs = workflowJobRepository.findByWorkflowId(workflowId);

        Set<UUID> completedIds = allJobs.stream()
                .filter(j -> j.getStatus() == JobStatus.COMPLETED)
                .map(WorkflowJob::getId)
                .collect(Collectors.toSet());

        List<WorkflowJob> runnable = allJobs.stream()
                .filter(j -> j.getStatus() == JobStatus.PENDING)
                .filter(j -> j.getDependencies().stream()
                        .allMatch(dep -> completedIds.contains(dep.getRequiredJob().getId())))
                .collect(Collectors.toList());

        if (runnable.isEmpty()) {

            boolean allDone = allJobs.stream().allMatch(j -> j.getStatus() == JobStatus.COMPLETED);
            if (allDone) {
                Workflow wf = workflowRepository.findById(workflowId).orElseThrow();
                wf.setStatus(WorkflowStatus.COMPLETED);
                workflowRepository.save(wf);
                log.info("Workflow {} completed", workflowId);
            }
            return;
        }

        for (WorkflowJob wj : runnable) {
            submitWorkflowJob(wj);
        }
    }

    private void submitWorkflowJob(WorkflowJob wj) {
        try {
            com.joborch.producer.dto.CreateJobRequest req = new com.joborch.producer.dto.CreateJobRequest();
            req.setName(wj.getWorkflow().getName() + "/" + wj.getStepName());
            req.setJobType(wj.getJobType());
            req.setPriority(wj.getPriority());
            req.setPayload(wj.getPayload());

            req.setIdempotencyKey("wf:" + wj.getWorkflow().getId() + ":step:" + wj.getId());

            com.joborch.producer.dto.JobResponse jobResp = jobService.createJob(req);
            wj.setJobId(jobResp.getId());
            wj.setStatus(JobStatus.QUEUED);
            workflowJobRepository.save(wj);
            log.info("Workflow step '{}' submitted as job {}", wj.getStepName(), jobResp.getId());

        } catch (Exception e) {
            log.error("Failed to submit workflow step '{}': {}", wj.getStepName(), e.getMessage());
        }
    }

    private void cancelDependents(UUID workflowId, UUID failedJobId) {
        workflowJobRepository.findByWorkflowId(workflowId).stream()
                .filter(j -> j.getStatus() == JobStatus.PENDING)
                .filter(j -> j.getDependencies().stream()
                        .anyMatch(dep -> dep.getRequiredJob().getId().equals(failedJobId)))
                .forEach(j -> {
                    j.setStatus(JobStatus.DEAD_LETTERED);
                    workflowJobRepository.save(j);
                    log.warn("Cancelled dependent step '{}' due to upstream failure", j.getStepName());
                });
    }

    private void validateNoCycles(List<WorkflowJob> jobs) {
        Map<UUID, Integer> inDegree = new HashMap<>();
        Map<UUID, List<UUID>> adj = new HashMap<>();

        for (WorkflowJob j : jobs) {
            inDegree.put(j.getId(), 0);
            adj.put(j.getId(), new ArrayList<>());
        }

        for (WorkflowJob j : jobs) {
            for (WorkflowJobDependency dep : j.getDependencies()) {
                UUID requiredId = dep.getRequiredJob().getId();
                adj.get(requiredId).add(j.getId());
                inDegree.merge(j.getId(), 1, Integer::sum);
            }
        }

        Queue<UUID> queue = new LinkedList<>();
        inDegree.forEach((id, degree) -> { if (degree == 0) queue.add(id); });

        int processed = 0;
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            processed++;
            for (UUID neighbor : adj.get(current)) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) queue.add(neighbor);
            }
        }

        if (processed != jobs.size()) {
            throw new IllegalArgumentException(
                    "Workflow contains a cycle — this is not a valid DAG. " +
                    "Processed " + processed + " of " + jobs.size() + " nodes.");
        }
        log.debug("Cycle detection passed for {} nodes", jobs.size());
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(UUID id) {
        return workflowRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new com.joborch.producer.exception.JobNotFoundException(id.toString()));
    }

    private WorkflowResponse toResponse(Workflow wf) {
        return WorkflowResponse.builder()
                .id(wf.getId())
                .name(wf.getName())
                .description(wf.getDescription())
                .status(wf.getStatus())
                .createdAt(wf.getCreatedAt())
                .build();
    }
}
