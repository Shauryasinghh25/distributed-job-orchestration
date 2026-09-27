package com.joborch.worker.service;

import com.joborch.common.domain.JobStatus;
import com.joborch.common.domain.JobStateMachine;
import com.joborch.common.messaging.JobMessage;
import com.joborch.worker.handler.JobHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobExecutionService {

    private final JdbcTemplate jdbcTemplate;
    private final JobHandlerRegistry handlerRegistry;

    @Value("${joborch.worker.retry.delays-ms:5000,30000,120000}")
    private String retryDelaysConfig;

    public ExecutionResult execute(JobMessage message) {
        UUID jobId = message.getJobId();
        log.info("Executing job: id={} type={} attempt={}", jobId, message.getJobType(), message.getAttemptNumber());

        JobStatus current = getJobStatus(jobId);
        if (current == null) {
            log.warn("Job {} not found — stale message, ACKing", jobId);
            return ExecutionResult.success();
        }
        if (JobStateMachine.isTerminal(current)) {
            log.info("Job {} already in terminal state {}. Idempotent ACK.", jobId, current);
            return ExecutionResult.success();
        }

        updateStatus(jobId, JobStatus.PROCESSING);

        try {
            handlerRegistry.handle(message);

            updateStatus(jobId, JobStatus.COMPLETED);
            log.info("Job {} COMPLETED on attempt {}", jobId, message.getAttemptNumber());
            return ExecutionResult.success();

        } catch (Exception e) {
            log.error("Job {} FAILED on attempt {}: {}", jobId, message.getAttemptNumber(), e.getMessage());

            int maxAttempts = getMaxAttempts(jobId);
            int attempt     = message.getAttemptNumber();

            if (attempt >= maxAttempts) {

                updateStatusWithError(jobId, JobStatus.DEAD_LETTERED, e.getMessage());
                return ExecutionResult.deadLetter("Exhausted " + maxAttempts + " attempts. Last: " + e.getMessage());
            } else {

                long delay = computeDelay(attempt);
                updateStatusWithError(jobId, JobStatus.RETRYING, e.getMessage());
                return ExecutionResult.retry(delay);
            }
        }
    }

    private long computeDelay(int attemptNumber) {
        long[] delays = parseDelays();
        int idx = Math.min(attemptNumber - 1, delays.length - 1);
        return delays[Math.max(0, idx)];
    }

    private long[] parseDelays() {
        try {
            String[] parts = retryDelaysConfig.split(",");
            long[] result = new long[parts.length];
            for (int i = 0; i < parts.length; i++) result[i] = Long.parseLong(parts[i].trim());
            return result;
        } catch (Exception e) {
            return new long[]{5000, 30000, 120000};
        }
    }

    private void updateStatus(UUID jobId, JobStatus status) {
        jdbcTemplate.update(
            "UPDATE jobs SET status = ?, updated_at = NOW() WHERE id = ?::uuid",
            status.name(), jobId.toString());
    }

    private void updateStatusWithError(UUID jobId, JobStatus status, String error) {
        String msg = error != null && error.length() > 1990 ? error.substring(0, 1990) + "..." : error;
        jdbcTemplate.update(
            "UPDATE jobs SET status = ?, error_message = ?, attempt_count = attempt_count + 1, updated_at = NOW() WHERE id = ?::uuid",
            status.name(), msg, jobId.toString());
    }

    private JobStatus getJobStatus(UUID jobId) {
        try {
            String s = jdbcTemplate.queryForObject(
                "SELECT status FROM jobs WHERE id = ?::uuid", String.class, jobId.toString());
            return s != null ? JobStatus.valueOf(s) : null;
        } catch (Exception e) { return null; }
    }

    private int getMaxAttempts(UUID jobId) {
        try {
            Integer v = jdbcTemplate.queryForObject(
                "SELECT max_attempts FROM jobs WHERE id = ?::uuid", Integer.class, jobId.toString());
            return v != null ? v : 3;
        } catch (Exception e) { return 3; }
    }
}
