package com.joborch.worker.handler;

import com.joborch.common.domain.JobType;
import com.joborch.common.messaging.JobMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReportJobHandler implements JobHandler {
    @Override
    public void handle(JobMessage message) throws Exception {
        String reportType = message.getPayload().getOrDefault("reportType", "UNKNOWN");
        log.info("[REPORT] Generating report={} jobId={}", reportType, message.getJobId());
        Thread.sleep(1500); 
        log.info("[REPORT] Report '{}' generated successfully", reportType);
    }
    public JobType getType() { return JobType.REPORT; }
}
