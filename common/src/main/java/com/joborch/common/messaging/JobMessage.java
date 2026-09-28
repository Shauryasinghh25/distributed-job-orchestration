package com.joborch.common.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.joborch.common.domain.JobPriority;
import com.joborch.common.domain.JobType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class JobMessage {

    @JsonProperty("jobId")
    private final UUID jobId;

    @JsonProperty("jobType")
    private final JobType jobType;

    @JsonProperty("priority")
    private final JobPriority priority;

    @JsonProperty("payload")
    private final Map<String, String> payload;

    @JsonProperty("enqueuedAt")
    private final Instant enqueuedAt;

    @JsonProperty("attemptNumber")
    private final int attemptNumber;

    @JsonCreator
    public JobMessage(
            @JsonProperty("jobId") UUID jobId,
            @JsonProperty("jobType") JobType jobType,
            @JsonProperty("priority") JobPriority priority,
            @JsonProperty("payload") Map<String, String> payload,
            @JsonProperty("enqueuedAt") Instant enqueuedAt,
            @JsonProperty("attemptNumber") int attemptNumber) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.priority = priority;
        this.payload = payload;
        this.enqueuedAt = enqueuedAt;
        this.attemptNumber = attemptNumber;
    }

    public static JobMessage forFirstAttempt(UUID jobId, JobType jobType,
                                              JobPriority priority, Map<String, String> payload) {
        return new JobMessage(jobId, jobType, priority, payload, Instant.now(), 1);
    }

    public JobMessage forRetry() {
        return new JobMessage(this.jobId, this.jobType, this.priority,
                this.payload, Instant.now(), this.attemptNumber + 1);
    }

    public UUID getJobId() { return jobId; }
    public JobType getJobType() { return jobType; }
    public JobPriority getPriority() { return priority; }
    public Map<String, String> getPayload() { return payload; }
    public Instant getEnqueuedAt() { return enqueuedAt; }
    public int getAttemptNumber() { return attemptNumber; }

    @Override
    public String toString() {
        return "JobMessage{" +
                "jobId=" + jobId +
                ", jobType=" + jobType +
                ", priority=" + priority +
                ", attemptNumber=" + attemptNumber +
                ", enqueuedAt=" + enqueuedAt +
                '}';
    }
}
