package com.joborch.producer.web;

import com.joborch.common.domain.JobStatus;
import com.joborch.producer.dto.CreateJobRequest;
import com.joborch.producer.dto.JobResponse;
import com.joborch.producer.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Slf4j
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.createJob(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<StatusResponse> getJobStatus(@PathVariable UUID id) {
        JobStatus status = jobService.getJobStatus(id);
        return ResponseEntity.ok(new StatusResponse(id, status));
    }

    record StatusResponse(UUID jobId, JobStatus status) {}
}
