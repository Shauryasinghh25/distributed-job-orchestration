package com.joborch.worker.handler;

import com.joborch.common.domain.JobType;
import com.joborch.common.messaging.JobMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataProcessingJobHandler implements JobHandler {
    @Override
    public void handle(JobMessage message) throws Exception {
        String batchId = message.getPayload().getOrDefault("batchId", "unknown");
        log.info("[DATA] Processing batch={} jobId={}", batchId, message.getJobId());
        Thread.sleep(800);
        log.info("[DATA] Batch {} processed", batchId);
    }
    public JobType getType() { return JobType.DATA_PROCESSING; }
}
